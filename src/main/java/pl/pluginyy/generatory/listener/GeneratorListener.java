package pl.pluginyy.generatory.listener;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.GameMode;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import pl.pluginyy.generatory.GeneratoryPlugin;
import pl.pluginyy.generatory.model.Generator;
import pl.pluginyy.generatory.model.GeneratorBlock;

import java.util.Iterator;
import java.util.Map;

/** Nagrody za kopanie w generatorach, zbieranie do ekwipunku i blokada stawiania. */
public final class GeneratorListener implements Listener {

    private final GeneratoryPlugin plugin;

    public GeneratorListener(GeneratoryPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Generator generator = plugin.generators().at(event.getBlock());
        if (generator == null) {
            return;
        }
        GeneratorBlock block = generator.block(event.getBlock().getType());
        if (block == null) {
            return;
        }
        Player player = event.getPlayer();
        if (!block.drops()) {
            event.setDropItems(false);
        }
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        if (block.xp() > 0) {
            event.setExpToDrop(event.getExpToDrop() + block.xp());
        }
        if (block.price() > 0 && plugin.vault().deposit(player, block.price())) {
            plugin.messages().actionBar(player, "zarobek-actionbar",
                    Placeholder.unparsed("kwota", plugin.vault().format(block.price())));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(BlockDropItemEvent event) {
        Generator generator = plugin.generators().at(event.getBlock());
        if (generator == null || !generator.autoPickup()) {
            return;
        }
        Player player = event.getPlayer();
        Iterator<Item> items = event.getItems().iterator();
        while (items.hasNext()) {
            Item item = items.next();
            Map<Integer, ItemStack> left = player.getInventory().addItem(item.getItemStack());
            if (left.isEmpty()) {
                items.remove();
            } else {
                item.setItemStack(left.values().iterator().next());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!plugin.getConfig().getBoolean("generator.blokuj-stawianie", true)
                || event.getPlayer().hasPermission("generatory.stawianie")) {
            return;
        }
        if (plugin.generators().at(event.getBlockPlaced()) != null) {
            event.setCancelled(true);
            plugin.messages().send(event.getPlayer(), "stawianie-zablokowane");
        }
    }
}
