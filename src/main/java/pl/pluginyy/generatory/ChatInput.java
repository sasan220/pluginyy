package pl.pluginyy.generatory;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Pyta gracza o wartość na czacie (np. proporcję albo cenę). */
public final class ChatInput implements Listener {

    private static final long TIMEOUT_TICKS = 20L * 60;

    private record Pending(Consumer<String> answer, Runnable cancel, BukkitTask timeout) {
    }

    private final GeneratoryPlugin plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public ChatInput(GeneratoryPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Zamyka GUI, wysyła pytanie i czeka na odpowiedź.
     * {@code cancel} jest wywoływane po wpisaniu "anuluj" albo po upływie czasu.
     */
    public void ask(Player player, Component question, Consumer<String> answer, Runnable cancel) {
        UUID id = player.getUniqueId();
        Pending old = pending.remove(id);
        if (old != null) {
            old.timeout().cancel();
        }
        player.closeInventory();
        player.sendMessage(question);
        BukkitTask timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Pending p = pending.remove(id);
            if (p != null) {
                plugin.messages().send(player, "czas-minal");
            }
        }, TIMEOUT_TICKS);
        pending.put(id, new Pending(answer, cancel, timeout));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Pending p = pending.remove(event.getPlayer().getUniqueId());
        if (p == null) {
            return;
        }
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> {
            p.timeout().cancel();
            if (text.equalsIgnoreCase("anuluj")) {
                plugin.messages().send(event.getPlayer(), "anulowano");
                p.cancel().run();
            } else {
                p.answer().accept(text);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Pending p = pending.remove(event.getPlayer().getUniqueId());
        if (p != null) {
            p.timeout().cancel();
        }
    }
}
