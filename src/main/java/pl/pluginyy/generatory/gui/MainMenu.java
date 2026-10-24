package pl.pluginyy.generatory.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import pl.pluginyy.generatory.GeneratoryPlugin;
import pl.pluginyy.generatory.RegenManager;
import pl.pluginyy.generatory.util.Text;

/** Główne menu: /generator */
public final class MainMenu extends Menu {

    public MainMenu(GeneratoryPlugin plugin) {
        super(plugin);
    }

    @Override
    protected Component title() {
        return Component.text("Generatory");
    }

    @Override
    protected void build(Player viewer) {
        RegenManager regen = plugin.regen();

        set(13, item(Material.CLOCK, "<gold>Czas odnawiania",
                "<gray>» Generatory odnawiają się co <white>" + Text.time(regen.interval()),
                "<gray>» Następne odnowienie za <white>" + Text.time(regen.secondsLeft()),
                "",
                "<gray>» <green>LPM</green> <gray>+10s, <red>PPM</red> <gray>-10s",
                "<gray>» <green>SHIFT + LPM</green> <gray>+1m, <red>SHIFT + PPM</red> <gray>-1m"), c -> {
            int delta = (c.shift() ? 60 : 10) * (c.left() ? 1 : -1);
            regen.setInterval(regen.interval() + delta);
            refresh(c.player());
        });

        set(20, item(Material.EMERALD_BLOCK, "<green>Lista generatorów",
                "<gray>» Generatorów: <white>" + plugin.generators().all().size(),
                "",
                "<gray>» Kliknij, aby zobaczyć listę"), c -> new ListMenu(plugin, 0).open(c.player()));

        set(22, item(Material.IRON_PICKAXE, "<yellow>Odnów wszystkie generatory",
                "<gray>» Kliknij, aby od razu odnowić",
                "<gray>  wszystkie generatory"), c -> {
            plugin.actions().regenerateAll(c.player());
            refresh(c.player());
        });

        set(24, item(Material.DIAMOND_HOE, "<aqua>Różdżka generatorów",
                "<gray>» Kliknij, aby otrzymać różdżkę generatorów",
                "",
                "<gray>» Zaznaczasz <aqua><b>LPM</b></aqua> <gray>pierwszą pozycję generatora",
                "<gray>» Zaznaczasz <yellow><b>PPM</b></yellow> <gray>drugą pozycję generatora"), c -> {
            c.player().closeInventory();
            plugin.actions().giveWand(c.player());
        });

        set(31, item(Material.BOOK, "<yellow>Jak stworzyć generator?",
                "<gray>1. Weź różdżkę (motyka obok)",
                "<gray>2. Zaznacz <aqua>LPM</aqua> <gray>i <yellow>PPM</yellow> <gray>dwa przeciwległe rogi",
                "<gray>3. Wpisz <yellow>/generator utworz [nazwa]",
                "<gray>4. Ustaw bloki, proporcje i ceny w GUI",
                "",
                "<gray>Generatory odnawiają się same co",
                "<gray>ustawiony czas – licznik widać na górze ekranu."));

        backButton("Zamknij", c -> c.player().closeInventory());
    }
}
