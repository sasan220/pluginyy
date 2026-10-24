package pl.pluginyy.generatory.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import pl.pluginyy.generatory.GeneratoryPlugin;
import pl.pluginyy.generatory.util.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Podstawa wszystkich GUI: 5 rzędów z ramką jak na filmiku
 * (zielone narożniki, kraty po bokach, bariera na dole).
 */
public abstract class Menu implements InventoryHolder {

    /** Wolne miejsca w środku ramki (3 rzędy po 7). */
    protected static final int[] CONTENT = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };
    protected static final int BACK_SLOT = 40;
    protected static final int PREV_SLOT = 38;
    protected static final int NEXT_SLOT = 42;

    private static final int[] CORNERS = {0, 1, 7, 8, 9, 17, 27, 35, 36, 37, 43, 44};
    private static final int[] EDGES = {2, 3, 5, 6, 38, 39, 41, 42};
    private static final int[] BARS = {4, 18, 26};

    /** Kliknięcie w GUI albo we własny ekwipunek, gdy GUI jest otwarte. */
    public record Click(Player player, ClickType type, int slot, ItemStack item) {
        public boolean left() {
            return type.isLeftClick();
        }

        public boolean right() {
            return type.isRightClick();
        }

        public boolean shift() {
            return type.isShiftClick();
        }
    }

    protected final GeneratoryPlugin plugin;
    private final Map<Integer, Consumer<Click>> actions = new HashMap<>();
    private Inventory inventory;

    protected Menu(GeneratoryPlugin plugin) {
        this.plugin = plugin;
    }

    /** GUI tego pluginu otwarte przez gracza albo null. */
    public static Menu openMenu(Player player) {
        Inventory top = player.getOpenInventory().getTopInventory();
        return top != null && top.getHolder() instanceof Menu menu ? menu : null;
    }

    protected abstract Component title();

    protected abstract void build(Player viewer);

    /** Kolor narożników ramki. */
    protected Material accent() {
        return Material.LIME_STAINED_GLASS_PANE;
    }

    public void open(Player player) {
        inventory = Bukkit.createInventory(this, 45, title());
        fill(player);
        player.openInventory(inventory);
    }

    /** Odświeża zawartość bez ponownego otwierania. */
    public void refresh(Player player) {
        if (inventory == null) {
            open(player);
            return;
        }
        fill(player);
    }

    private void fill(Player player) {
        inventory.clear();
        actions.clear();
        ItemStack accent = pane(accent());
        ItemStack edge = pane(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemStack bars = pane(Material.IRON_BARS);
        for (int slot : CORNERS) {
            inventory.setItem(slot, accent);
        }
        for (int slot : EDGES) {
            inventory.setItem(slot, edge);
        }
        for (int slot : BARS) {
            inventory.setItem(slot, bars);
        }
        build(player);
    }

    protected void set(int slot, ItemStack item, Consumer<Click> action) {
        inventory.setItem(slot, item);
        if (action != null) {
            actions.put(slot, action);
        } else {
            actions.remove(slot);
        }
    }

    protected void set(int slot, ItemStack item) {
        set(slot, item, null);
    }

    protected void backButton(String label, Consumer<Click> action) {
        set(BACK_SLOT, item(Material.BARRIER, "<red>" + label), action);
    }

    /** Strzałki stron. Zwraca listę elementów na bieżącej stronie. */
    protected <T> List<T> paginate(List<T> all, int page, Consumer<Integer> openPage) {
        int pages = pages(all.size());
        int current = Math.max(0, Math.min(page, pages - 1));
        if (current > 0) {
            set(PREV_SLOT, item(Material.ARROW, "<yellow>« Poprzednia strona"), c -> openPage.accept(current - 1));
        }
        if (current < pages - 1) {
            set(NEXT_SLOT, item(Material.ARROW, "<yellow>Następna strona »"), c -> openPage.accept(current + 1));
        }
        int from = current * CONTENT.length;
        return all.subList(Math.min(from, all.size()), Math.min(from + CONTENT.length, all.size()));
    }

    protected static int pages(int size) {
        return Math.max(1, (size + CONTENT.length - 1) / CONTENT.length);
    }

    public void handleClick(Click click) {
        Consumer<Click> action = actions.get(click.slot());
        if (action != null) {
            action.accept(click);
        }
    }

    /** Kliknięcie we własny ekwipunek (np. wybór bloku). Domyślnie nic nie robi. */
    public void handleInventoryClick(Click click) {
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    // ---- przedmioty ----

    protected static ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(Component.empty());
            meta.setHideTooltip(true);
        });
        return item;
    }

    protected static ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(Text.item(name));
            if (lore.length > 0) {
                meta.lore(Text.lore(lore));
            }
            meta.addItemFlags(ItemFlag.values());
        });
        return item;
    }

    protected static ItemStack item(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(name);
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.values());
        });
        return item;
    }

    /** Przedmiot do pokazania bloku w GUI (niektóre bloki, np. woda, nie mają przedmiotu). */
    protected static Material icon(Material material) {
        return material.isItem() && !material.isAir() ? material : Material.BARRIER;
    }
}
