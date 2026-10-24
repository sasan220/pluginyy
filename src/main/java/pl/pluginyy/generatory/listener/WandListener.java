package pl.pluginyy.generatory.listener;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import pl.pluginyy.generatory.GeneratoryPlugin;
import pl.pluginyy.generatory.model.Selection;

public final class WandListener implements Listener {

    private final GeneratoryPlugin plugin;

    public WandListener(GeneratoryPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !plugin.wand().isWand(event.getItem())) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("generatory.admin")) {
            return;
        }
        Action action = event.getAction();
        Block block = event.getClickedBlock();
        if (block == null || (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        event.setCancelled(true);

        Selection sel = plugin.selections().get(player);
        boolean first = action == Action.LEFT_CLICK_BLOCK;
        if (first) {
            if (block.getLocation().equals(sel.pos1())) {
                return; // przytrzymany LPM wywołuje zdarzenie kilka razy
            }
            sel.pos1(block.getLocation());
        } else {
            sel.pos2(block.getLocation());
        }
        plugin.messages().send(player, first ? "punkt-1" : "punkt-2",
                Placeholder.unparsed("x", String.valueOf(block.getX())),
                Placeholder.unparsed("y", String.valueOf(block.getY())),
                Placeholder.unparsed("z", String.valueOf(block.getZ())));
        if (sel.sameWorld()) {
            plugin.messages().send(player, "rozmiar-zaznaczenia",
                    Placeholder.unparsed("wymiary", sel.dimensions()),
                    Placeholder.unparsed("bloki", String.valueOf(sel.volume())));
        }
    }

    /** W trybie kreatywnym LPM różdżką niszczyłby blok. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("generatory.admin")
                && plugin.wand().isWand(player.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
        }
    }
}
