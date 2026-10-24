package pl.pluginyy.generatory;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.pluginyy.generatory.model.Selection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SelectionManager implements Listener {

    private final Map<UUID, Selection> selections = new HashMap<>();

    public Selection get(Player player) {
        return selections.computeIfAbsent(player.getUniqueId(), id -> new Selection());
    }

    /** Zaznaczenie bez tworzenia nowego (może zwrócić null). */
    public Selection peek(Player player) {
        return selections.get(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        selections.remove(event.getPlayer().getUniqueId());
    }
}
