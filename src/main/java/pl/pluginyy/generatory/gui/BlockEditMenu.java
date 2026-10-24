package pl.pluginyy.generatory.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import pl.pluginyy.generatory.GeneratoryPlugin;
import pl.pluginyy.generatory.model.Generator;
import pl.pluginyy.generatory.model.GeneratorBlock;
import pl.pluginyy.generatory.util.Text;

import java.util.function.Consumer;

/** "Edycja: blok" – cena, doświadczenie i wypadanie jednego bloku z cennika. */
public final class BlockEditMenu extends Menu {

    private final Generator generator;
    private final Material material;

    public BlockEditMenu(GeneratoryPlugin plugin, Generator generator, Material material) {
        super(plugin);
        this.generator = generator;
        this.material = material;
    }

    @Override
    protected Component title() {
        return Component.text("Edycja: ").append(Text.blockName(material));
    }

    @Override
    protected Material accent() {
        return Material.LIGHT_BLUE_STAINED_GLASS_PANE;
    }

    @Override
    protected void build(Player viewer) {
        GeneratorBlock block = generator.block(material);
        if (block == null) {
            backButton("Cofnij", c -> new PriceMenu(plugin, generator).open(c.player()));
            return;
        }

        set(13, item(icon(material), Text.itemBlockName(material), Text.lore(
                "<gray>» Generator: <white>" + generator.name())));

        set(20, item(Material.EMERALD, "<green>Cena za wykopanie: <white>" + plugin.vault().format(block.price()),
                "<gray>» Gracz dostaje tyle pieniędzy",
                "<gray>  za każdy wykopany blok",
                "",
                "<gray>» Kliknij <green>LPM</green><gray>, aby ustawić cenę",
                "<gray>» Kliknij <red>PPM</red><gray>, aby wyzerować"), c -> {
            if (c.right()) {
                update(c.player(), b -> b.price(0));
            } else {
                ask(c.player(), "wpisz-cene", value -> value >= 0 && value <= 1_000_000_000, (b, value) -> b.price(value));
            }
        });

        set(22, item(Material.EXPERIENCE_BOTTLE, "<green>Doświadczenie: <white>" + block.xp(),
                "<gray>» Ile punktów doświadczenia",
                "<gray>  wypada z bloku",
                "",
                "<gray>» Kliknij <green>LPM</green><gray>, aby ustawić",
                "<gray>» Kliknij <red>PPM</red><gray>, aby wyzerować"), c -> {
            if (c.right()) {
                update(c.player(), b -> b.xp(0));
            } else {
                ask(c.player(), "wpisz-xp", value -> value >= 0 && value <= 10_000, (b, value) -> b.xp((int) Math.round(value)));
            }
        });

        set(24, item(Material.CHEST, "<green>Wypadanie przedmiotu: " + (block.drops() ? "<green>TAK" : "<red>NIE"),
                "<gray>» Czy z wykopanego bloku",
                "<gray>  wypada przedmiot",
                "",
                "<gray>» Kliknij, aby przełączyć"), c -> update(c.player(), b -> b.drops(!b.drops())));

        backButton("Cofnij", c -> new PriceMenu(plugin, generator).open(c.player()));
    }

    private interface Setter {
        void apply(GeneratorBlock block, double value);
    }

    private interface Check {
        boolean ok(double value);
    }

    private void update(Player player, Consumer<GeneratorBlock> change) {
        GeneratorBlock block = generator.block(material);
        if (block == null || !plugin.generators().exists(generator)) {
            new ListMenu(plugin, 0).open(player);
            return;
        }
        change.accept(block);
        plugin.generators().save();
        refresh(player);
    }

    private void ask(Player player, String messageKey, Check check, Setter setter) {
        plugin.chatInput().ask(player,
                plugin.messages().get(messageKey, Placeholder.component("blok", Text.blockName(material))),
                text -> {
                    Double value = Text.parseNumber(text);
                    GeneratorBlock block = generator.block(material);
                    if (value == null || !check.ok(value)) {
                        plugin.messages().send(player, "zla-liczba");
                    } else if (block != null && plugin.generators().exists(generator)) {
                        setter.apply(block, value);
                        plugin.generators().save();
                        plugin.messages().send(player, "zapisano");
                    }
                    reopen(player);
                },
                () -> reopen(player));
    }

    private void reopen(Player player) {
        if (!player.isOnline()) {
            return;
        }
        if (plugin.generators().exists(generator) && generator.hasBlock(material)) {
            new BlockEditMenu(plugin, generator, material).open(player);
        } else {
            new ListMenu(plugin, 0).open(player);
        }
    }
}
