package pl.pluginyy.generatory.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.pluginyy.generatory.GeneratoryPlugin;
import pl.pluginyy.generatory.model.Generator;
import pl.pluginyy.generatory.model.GeneratorBlock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Lista wszystkich generatorów. */
public final class ListMenu extends Menu {

    private final int page;

    public ListMenu(GeneratoryPlugin plugin, int page) {
        super(plugin);
        int pages = pages(plugin.generators().all().size());
        this.page = Math.max(0, Math.min(page, pages - 1));
    }

    @Override
    protected Component title() {
        int pages = pages(plugin.generators().all().size());
        return Component.text("Lista generatorów (" + (page + 1) + "/" + pages + ")");
    }

    @Override
    protected void build(Player viewer) {
        List<Generator> all = new ArrayList<>(plugin.generators().all());
        List<Generator> shown = paginate(all, page, p -> new ListMenu(plugin, p).open(viewer));

        for (int i = 0; i < shown.size(); i++) {
            Generator g = shown.get(i);
            set(CONTENT[i], icon(g), c -> {
                if (!plugin.generators().exists(g)) {
                    new ListMenu(plugin, page).open(c.player());
                } else if (c.right() && c.shift()) {
                    plugin.actions().regenerate(c.player(), g);
                } else if (c.right()) {
                    c.player().closeInventory();
                    plugin.actions().teleport(c.player(), g);
                } else {
                    new EditMenu(plugin, g).open(c.player());
                }
            });
        }

        if (all.isEmpty()) {
            set(22, item(Material.PAPER, "<gray>Brak generatorów",
                    "<gray>Zaznacz obszar różdżką i wpisz",
                    "<yellow>/generator utworz [nazwa]"));
        }

        backButton("Cofnij", c -> new MainMenu(plugin).open(c.player()));
    }

    private ItemStack icon(Generator g) {
        Iterator<GeneratorBlock> blocks = g.blocks().iterator();
        Material icon = blocks.hasNext() ? icon(blocks.next().material()) : Material.GRASS_BLOCK;
        String blockInfo = g.blocks().isEmpty()
                ? "<red>» Brak bloków – ustaw je w edycji!"
                : "<gray>» Rodzaje bloków: <white>" + g.blocks().size();
        return item(icon, "<yellow>" + g.name(),
                "<gray>» Świat: <white>" + g.worldName(),
                "<gray>» Od: <white>" + g.minX() + " " + g.minY() + " " + g.minZ(),
                "<gray>» Do: <white>" + g.maxX() + " " + g.maxY() + " " + g.maxZ(),
                "<gray>» Rozmiar: <white>" + g.dimensions() + " <dark_gray>(" + g.volume() + " bloków)",
                blockInfo,
                "",
                "<gray>» <green>LPM</green> <gray>– edytuj",
                "<gray>» <yellow>PPM</yellow> <gray>– teleportuj",
                "<gray>» <aqua>SHIFT + PPM</aqua> <gray>– odnów teraz");
    }
}
