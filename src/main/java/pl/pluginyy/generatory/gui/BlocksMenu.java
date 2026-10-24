package pl.pluginyy.generatory.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.pluginyy.generatory.GeneratorManager;
import pl.pluginyy.generatory.GeneratoryPlugin;
import pl.pluginyy.generatory.model.Generator;
import pl.pluginyy.generatory.model.GeneratorBlock;
import pl.pluginyy.generatory.util.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * "Bloki: nazwa" – z czego składa się generator.
 * LPM proporcja, SHIFT + LPM zamiana bloku, PPM usunięcie; klik w ekwipunek dodaje blok.
 */
public final class BlocksMenu extends Menu {

    private final Generator generator;
    private Material replacing;

    public BlocksMenu(GeneratoryPlugin plugin, Generator generator) {
        super(plugin);
        this.generator = generator;
    }

    @Override
    protected Component title() {
        return Component.text("Bloki: " + generator.name());
    }

    @Override
    protected void build(Player viewer) {
        List<GeneratorBlock> blocks = new ArrayList<>(generator.blocks());
        int shown = Math.min(blocks.size(), GeneratorManager.MAX_BLOCK_TYPES);
        for (int i = 0; i < shown; i++) {
            GeneratorBlock block = blocks.get(i);
            set(CONTENT[i], blockItem(block), c -> click(c, block));
        }
        if (shown < GeneratorManager.MAX_BLOCK_TYPES) {
            set(CONTENT[shown], item(Material.LIME_DYE, "<green>Dodaj blok",
                    "<gray>» Kliknij dowolny blok w swoim",
                    "<gray>  ekwipunku, aby dodać go do generatora",
                    "",
                    "<dark_gray>Proporcje działają jak wagi:",
                    "<dark_gray>np. 70 i 30 to 70% i 30%."));
        }
        backButton("Cofnij", c -> new EditMenu(plugin, generator).open(c.player()));
    }

    private ItemStack blockItem(GeneratorBlock block) {
        Material material = block.material();
        List<Component> lore = new ArrayList<>(Text.lore(
                "<gray>» Proporcja: <green>" + Text.percent(generator.share(block))
                        + " <dark_gray>(waga: " + Text.number(block.weight()) + ")",
                "",
                "<gray>» Kliknij <green>LPM</green><gray>, aby ustawić proporcję",
                "<gray>» Kliknij <yellow>SHIFT + LPM</yellow><gray>, aby zmienić blok",
                "<gray>» Kliknij <red>PPM</red><gray>, aby usunąć blok"));
        if (material == replacing) {
            lore.add(Component.empty());
            lore.add(Text.item("<yellow>Wybierz nowy blok w swoim ekwipunku..."));
        }
        ItemStack item = item(icon(material), Text.itemBlockName(material), lore);
        if (material == replacing) {
            item.editMeta(meta -> meta.setEnchantmentGlintOverride(true));
        }
        return item;
    }

    private void click(Click c, GeneratorBlock block) {
        Player player = c.player();
        if (!plugin.generators().exists(generator)) {
            new ListMenu(plugin, 0).open(player);
            return;
        }
        if (c.right()) {
            generator.removeBlock(block.material());
            replacing = null;
            plugin.generators().save();
            refresh(player);
        } else if (c.shift()) {
            replacing = block.material();
            plugin.messages().send(player, "wybierz-zamiennik",
                    Placeholder.component("blok", Text.blockName(block.material())));
            refresh(player);
        } else {
            plugin.chatInput().ask(player,
                    plugin.messages().get("wpisz-proporcje", Placeholder.component("blok", Text.blockName(block.material()))),
                    text -> {
                        Double value = Text.parseNumber(text);
                        if (value == null || value < 0 || value > 1_000_000) {
                            plugin.messages().send(player, "zla-liczba");
                        } else {
                            block.weight(value);
                            generator.invalidate();
                            plugin.generators().save();
                            plugin.messages().send(player, "zapisano");
                        }
                        reopen(player);
                    },
                    () -> reopen(player));
        }
    }

    @Override
    public void handleInventoryClick(Click c) {
        Player player = c.player();
        ItemStack item = c.item();
        if (item == null || item.getType().isAir() || !plugin.generators().exists(generator)) {
            return;
        }
        Material material = item.getType();
        if (!material.isBlock()) {
            plugin.messages().send(player, "to-nie-blok");
            return;
        }
        if (generator.hasBlock(material) && material != replacing) {
            plugin.messages().send(player, "blok-juz-jest");
            return;
        }
        if (replacing != null) {
            if (generator.hasBlock(replacing)) {
                generator.replaceBlock(replacing, material);
            }
            replacing = null;
        } else {
            if (generator.blocks().size() >= GeneratorManager.MAX_BLOCK_TYPES) {
                plugin.messages().send(player, "limit-blokow",
                        Placeholder.unparsed("maks", String.valueOf(GeneratorManager.MAX_BLOCK_TYPES)));
                return;
            }
            generator.addBlock(new GeneratorBlock(material, 100, 0, 0, true));
            plugin.messages().send(player, "dodano-blok", Placeholder.component("blok", Text.blockName(material)));
        }
        plugin.generators().save();
        refresh(player);
    }

    private void reopen(Player player) {
        if (!player.isOnline()) {
            return;
        }
        if (plugin.generators().exists(generator)) {
            new BlocksMenu(plugin, generator).open(player);
        } else {
            new ListMenu(plugin, 0).open(player);
        }
    }
}
