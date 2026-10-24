package pl.pluginyy.generatory.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import pl.pluginyy.generatory.GeneratoryPlugin;
import pl.pluginyy.generatory.model.Generator;
import pl.pluginyy.generatory.model.GeneratorBlock;
import pl.pluginyy.generatory.util.Text;

import java.util.ArrayList;
import java.util.List;

/** "Cennik: nazwa" – nagrody za wykopanie każdego bloku. */
public final class PriceMenu extends Menu {

    private final Generator generator;

    public PriceMenu(GeneratoryPlugin plugin, Generator generator) {
        super(plugin);
        this.generator = generator;
    }

    @Override
    protected Component title() {
        return Component.text("Cennik: " + generator.name());
    }

    @Override
    protected Material accent() {
        return Material.LIGHT_BLUE_STAINED_GLASS_PANE;
    }

    @Override
    protected void build(Player viewer) {
        if (!plugin.vault().available()) {
            set(4, item(Material.GOLD_INGOT, "<red>Brak ekonomii",
                    "<gray>Zainstaluj <white>Vault</white> <gray>i plugin ekonomii",
                    "<gray>(np. <white>EssentialsX</white><gray>), aby ceny działały.",
                    "<gray>Doświadczenie i wypadanie działają bez tego."));
        }

        List<GeneratorBlock> blocks = new ArrayList<>(generator.blocks());
        int shown = Math.min(blocks.size(), CONTENT.length);
        for (int i = 0; i < shown; i++) {
            GeneratorBlock block = blocks.get(i);
            Material material = block.material();
            set(CONTENT[i], item(icon(material), Text.itemBlockName(material), Text.lore(
                    "<gray>» Cena: " + (block.price() > 0 ? "<green>" + plugin.vault().format(block.price()) : "<dark_gray>brak"),
                    "<gray>» Doświadczenie: " + (block.xp() > 0 ? "<green>" + block.xp() : "<dark_gray>brak"),
                    "<gray>» Wypadanie przedmiotu: " + (block.drops() ? "<green>TAK" : "<red>NIE"),
                    "",
                    "<gray>» Kliknij, aby edytować")), c -> {
                if (plugin.generators().exists(generator) && generator.hasBlock(material)) {
                    new BlockEditMenu(plugin, generator, material).open(c.player());
                } else {
                    refresh(c.player());
                }
            });
        }
        if (blocks.isEmpty()) {
            set(22, item(Material.PAPER, "<gray>Brak bloków",
                    "<gray>Najpierw dodaj bloki w menu",
                    "<gray>„Bloki i proporcje”."));
        }

        backButton("Cofnij", c -> new EditMenu(plugin, generator).open(c.player()));
    }
}
