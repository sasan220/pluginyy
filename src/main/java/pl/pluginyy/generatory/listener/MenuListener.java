package pl.pluginyy.generatory.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.pluginyy.generatory.GeneratoryPlugin;
import pl.pluginyy.generatory.gui.Menu;

public final class MenuListener implements Listener {

    private final GeneratoryPlugin plugin;

    public MenuListener(GeneratoryPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof Menu menu)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() == null) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        Menu.Click click = new Menu.Click(player, event.getClick(), event.getRawSlot(),
                current == null ? null : current.clone());
        boolean inMenu = event.getRawSlot() < top.getSize();
        // Akcje (otwieranie innych GUI, zamykanie) wykonujemy w następnym ticku – tak zaleca Bukkit
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (Menu.openMenu(player) != menu) {
                return; // gracz zdążył zamknąć GUI
            }
            if (inMenu) {
                menu.handleClick(click);
            } else {
                menu.handleInventoryClick(click);
            }
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) {
            event.setCancelled(true);
        }
    }
}
