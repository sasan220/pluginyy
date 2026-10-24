package pl.pluginyy.generatory.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import pl.pluginyy.generatory.GeneratoryPlugin;
import pl.pluginyy.generatory.model.Generator;

/** Edycja jednego generatora. */
public final class EditMenu extends Menu {

    private final Generator generator;

    public EditMenu(GeneratoryPlugin plugin, Generator generator) {
        super(plugin);
        this.generator = generator;
    }

    @Override
    protected Component title() {
        return Component.text("Edycja: " + generator.name());
    }

    @Override
    protected void build(Player viewer) {
        Generator g = generator;

        set(10, item(Material.OAK_SIGN, "<yellow>" + g.name(),
                "<gray>» Świat: <white>" + g.worldName(),
                "<gray>» Od: <white>" + g.minX() + " " + g.minY() + " " + g.minZ(),
                "<gray>» Do: <white>" + g.maxX() + " " + g.maxY() + " " + g.maxZ(),
                "<gray>» Rozmiar: <white>" + g.dimensions() + " <dark_gray>(" + g.volume() + " bloków)"));

        set(12, item(Material.CHEST, "<green>Bloki i proporcje",
                "<gray>» Ustaw, z jakich bloków składa się",
                "<gray>  generator i jak często się pojawiają",
                "",
                "<gray>» Kliknij, aby edytować"), c -> open(c, new BlocksMenu(plugin, g)));

        set(14, item(Material.EMERALD, "<green>Cennik",
                "<gray>» Ustaw cenę za wykopanie, doświadczenie",
                "<gray>  i wypadanie przedmiotu dla każdego bloku",
                "",
                "<gray>» Kliknij, aby edytować"), c -> open(c, new PriceMenu(plugin, g)));

        set(16, item(Material.TNT, "<red>Odnów teraz",
                "<gray>» Kliknij, aby od razu odnowić",
                "<gray>  ten generator"), c -> {
            if (alive(c)) {
                plugin.actions().regenerate(c.player(), g);
            }
        });

        set(20, item(Material.ENDER_PEARL, "<aqua>Teleportuj",
                "<gray>» Przenosi Cię na górę generatora"), c -> {
            if (alive(c)) {
                c.player().closeInventory();
                plugin.actions().teleport(c.player(), g);
            }
        });

        set(22, item(Material.DIAMOND_HOE, "<aqua>Pokaż obszar",
                "<gray>» Wczytuje obszar generatora do",
                "<gray>  Twojego zaznaczenia (podgląd z cząsteczek)"), c -> {
            if (alive(c)) {
                c.player().closeInventory();
                plugin.actions().loadIntoSelection(c.player(), g);
            }
        });

        set(24, item(Material.ANVIL, "<yellow>Przenieś na zaznaczenie",
                "<gray>» Ustawia nowy obszar generatora",
                "<gray>  z Twojego zaznaczenia różdżką"), c -> {
            if (alive(c) && plugin.actions().move(c.player(), g)) {
                refresh(c.player());
            }
        });

        set(30, item(Material.HOPPER, "<gold>Zbieranie do ekwipunku: " + (g.autoPickup() ? "<green>TAK" : "<red>NIE"),
                "<gray>» Wykopane bloki trafiają od razu",
                "<gray>  do ekwipunku gracza",
                "",
                "<gray>» Kliknij, aby przełączyć"), c -> {
            if (alive(c)) {
                g.autoPickup(!g.autoPickup());
                plugin.generators().save();
                refresh(c.player());
            }
        });

        set(32, item(Material.LAVA_BUCKET, "<red>Usuń generator",
                "<gray>» Kliknij <red>SHIFT + PPM</red><gray>, aby usunąć",
                "<red>  Tej operacji nie da się cofnąć!"), c -> {
            if (alive(c) && c.shift() && c.right()) {
                plugin.actions().delete(c.player(), g);
                new ListMenu(plugin, 0).open(c.player());
            }
        });

        backButton("Cofnij", c -> new ListMenu(plugin, 0).open(c.player()));
    }

    private void open(Click c, Menu menu) {
        if (alive(c)) {
            menu.open(c.player());
        }
    }

    /** Sprawdza, czy generator nie został w międzyczasie usunięty. */
    private boolean alive(Click c) {
        if (plugin.generators().exists(generator)) {
            return true;
        }
        new ListMenu(plugin, 0).open(c.player());
        return false;
    }
}
