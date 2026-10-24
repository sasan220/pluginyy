package pl.pluginyy.generatory;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.pluginyy.generatory.model.Generator;
import pl.pluginyy.generatory.model.GeneratorBlock;
import pl.pluginyy.generatory.model.Selection;

/** Operacje na generatorach wspólne dla komend i GUI. */
public final class Actions {

    private final GeneratoryPlugin plugin;

    public Actions(GeneratoryPlugin plugin) {
        this.plugin = plugin;
    }

    private Messages msg() {
        return plugin.messages();
    }

    public void giveWand(Player player) {
        for (ItemStack left : player.getInventory().addItem(plugin.wand().create()).values()) {
            player.getWorld().dropItem(player.getLocation(), left);
        }
        msg().send(player, "otrzymano-rozdzke");
    }

    /** Tworzy generator z zaznaczenia gracza. Zwraca null, gdy się nie udało. */
    public Generator create(Player player, String name) {
        if (!GeneratorManager.NAME_PATTERN.matcher(name).matches()) {
            msg().send(player, "zly-format-nazwy");
            return null;
        }
        if (plugin.generators().get(name) != null) {
            msg().send(player, "nazwa-zajeta", Placeholder.unparsed("nazwa", name));
            return null;
        }
        Selection sel = validSelection(player, null);
        if (sel == null) {
            return null;
        }
        Location a = sel.pos1();
        Location b = sel.pos2();
        Generator generator = new Generator(name, a.getWorld().getName(),
                a.getBlockX(), a.getBlockY(), a.getBlockZ(), b.getBlockX(), b.getBlockY(), b.getBlockZ());
        generator.addBlock(new GeneratorBlock(Material.STONE, 100, 0, 0, true));
        plugin.generators().add(generator);
        sel.clear();
        msg().send(player, "utworzono",
                Placeholder.unparsed("nazwa", generator.name()),
                Placeholder.unparsed("wymiary", generator.dimensions()),
                Placeholder.unparsed("bloki", String.valueOf(generator.volume())));
        return generator;
    }

    /** Ustawia generatorowi nowy obszar z zaznaczenia gracza. */
    public boolean move(Player player, Generator generator) {
        Selection sel = validSelection(player, generator);
        if (sel == null) {
            return false;
        }
        Location a = sel.pos1();
        Location b = sel.pos2();
        plugin.regen().cancel(generator);
        generator.setArea(a.getWorld().getName(),
                a.getBlockX(), a.getBlockY(), a.getBlockZ(), b.getBlockX(), b.getBlockY(), b.getBlockZ());
        plugin.generators().save();
        sel.clear();
        msg().send(player, "przeniesiono",
                Placeholder.unparsed("nazwa", generator.name()),
                Placeholder.unparsed("wymiary", generator.dimensions()));
        return true;
    }

    private Selection validSelection(Player player, Generator except) {
        Selection sel = plugin.selections().peek(player);
        if (sel == null || !sel.complete()) {
            msg().send(player, "brak-zaznaczenia");
            return null;
        }
        if (!sel.sameWorld()) {
            msg().send(player, "rozne-swiaty");
            return null;
        }
        long max = plugin.getConfig().getLong("generator.maks-rozmiar", 250000);
        if (sel.volume() > max) {
            msg().send(player, "za-duzy",
                    Placeholder.unparsed("bloki", String.valueOf(sel.volume())),
                    Placeholder.unparsed("maks", String.valueOf(max)));
            return null;
        }
        Location a = sel.pos1();
        Location b = sel.pos2();
        Generator other = plugin.generators().overlapping(a.getWorld().getName(),
                a.getBlockX(), a.getBlockY(), a.getBlockZ(), b.getBlockX(), b.getBlockY(), b.getBlockZ(), except);
        if (other != null) {
            msg().send(player, "nachodzi", Placeholder.unparsed("nazwa", other.name()));
            return null;
        }
        return sel;
    }

    public void delete(CommandSender sender, Generator generator) {
        plugin.regen().cancel(generator);
        plugin.generators().remove(generator);
        msg().send(sender, "usunieto", Placeholder.unparsed("nazwa", generator.name()));
    }

    public void teleport(Player player, Generator generator) {
        World world = generator.world();
        if (world == null) {
            msg().send(player, "swiat-niedostepny");
            return;
        }
        Location target = generator.topCenter(world);
        target.setYaw(player.getLocation().getYaw());
        target.setPitch(player.getLocation().getPitch());
        player.teleport(target);
        msg().send(player, "teleport", Placeholder.unparsed("nazwa", generator.name()));
    }

    public void regenerate(CommandSender sender, Generator generator) {
        if (generator.world() == null) {
            msg().send(sender, "swiat-niedostepny");
        } else if (!plugin.regen().regenerate(generator)) {
            msg().send(sender, "brak-blokow", Placeholder.unparsed("nazwa", generator.name()));
        } else {
            msg().send(sender, "odnawianie-jednego", Placeholder.unparsed("nazwa", generator.name()));
        }
    }

    public void regenerateAll(CommandSender sender) {
        plugin.regen().regenerateAll(true);
        msg().send(sender, "odnawianie-wszystkich");
    }

    /** Wczytuje obszar generatora do zaznaczenia gracza, żeby zobaczył go w podglądzie. */
    public void loadIntoSelection(Player player, Generator generator) {
        World world = generator.world();
        if (world == null) {
            msg().send(player, "swiat-niedostepny");
            return;
        }
        Selection sel = plugin.selections().get(player);
        sel.pos1(new Location(world, generator.minX(), generator.minY(), generator.minZ()));
        sel.pos2(new Location(world, generator.maxX(), generator.maxY(), generator.maxZ()));
        boolean hasWand = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (plugin.wand().isWand(item)) {
                hasWand = true;
                break;
            }
        }
        if (!hasWand) {
            giveWand(player);
        }
        msg().send(player, "wczytano-obszar", Placeholder.unparsed("nazwa", generator.name()));
    }
}
