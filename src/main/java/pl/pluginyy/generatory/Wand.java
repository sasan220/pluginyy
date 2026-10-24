package pl.pluginyy.generatory;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import pl.pluginyy.generatory.util.Text;

/** Różdżka generatorów: LPM ustawia punkt 1, PPM ustawia punkt 2. */
public final class Wand {

    private final NamespacedKey key;

    public Wand(GeneratoryPlugin plugin) {
        this.key = new NamespacedKey(plugin, "rozdzka");
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(Material.DIAMOND_HOE);
        item.editMeta(meta -> {
            meta.displayName(Text.item("<aqua>Różdżka generatorów"));
            meta.lore(Text.lore(
                    "<gray>» Zaznaczasz <aqua><b>LPM</b></aqua> <gray>pierwszą pozycję generatora",
                    "<gray>» Zaznaczasz <yellow><b>PPM</b></yellow> <gray>drugą pozycję generatora"));
            meta.setUnbreakable(true);
            meta.setEnchantmentGlintOverride(true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        });
        return item;
    }

    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND_HOE || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
