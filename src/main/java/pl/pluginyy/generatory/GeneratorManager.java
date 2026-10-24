package pl.pluginyy.generatory;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.pluginyy.generatory.model.Generator;
import pl.pluginyy.generatory.model.GeneratorBlock;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Pattern;

/** Przechowuje generatory i zapisuje je do generators.yml. */
public final class GeneratorManager {

    public static final Pattern NAME_PATTERN = Pattern.compile("[\\p{L}0-9_-]{1,32}");
    public static final int MAX_BLOCK_TYPES = 20;

    private final GeneratoryPlugin plugin;
    private final File file;
    private final Map<String, Generator> generators = new LinkedHashMap<>();

    public GeneratorManager(GeneratoryPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "generators.yml");
    }

    public void load() {
        generators.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yml.getConfigurationSection("generatory");
        if (root == null) {
            return;
        }
        for (String name : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(name);
            if (sec == null) {
                continue;
            }
            Generator generator = new Generator(name, sec.getString("swiat", "world"),
                    sec.getInt("min.x"), sec.getInt("min.y"), sec.getInt("min.z"),
                    sec.getInt("max.x"), sec.getInt("max.y"), sec.getInt("max.z"));
            generator.autoPickup(sec.getBoolean("zbieranie-do-ekwipunku", false));

            ConfigurationSection blocks = sec.getConfigurationSection("bloki");
            if (blocks != null) {
                for (String materialName : blocks.getKeys(false)) {
                    Material material = Material.matchMaterial(materialName);
                    if (material == null || !material.isBlock()) {
                        plugin.getLogger().warning("Generator " + name + ": nieznany blok " + materialName + " – pomijam.");
                        continue;
                    }
                    generator.addBlock(new GeneratorBlock(material,
                            blocks.getDouble(materialName + ".waga", 100),
                            blocks.getDouble(materialName + ".cena", 0),
                            blocks.getInt(materialName + ".xp", 0),
                            blocks.getBoolean(materialName + ".wypadanie", true)));
                }
            }
            generators.put(generator.key(), generator);
        }
        plugin.getLogger().info("Wczytano generatorów: " + generators.size());
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Generator g : generators.values()) {
            String path = "generatory." + g.name();
            yml.set(path + ".swiat", g.worldName());
            yml.set(path + ".min.x", g.minX());
            yml.set(path + ".min.y", g.minY());
            yml.set(path + ".min.z", g.minZ());
            yml.set(path + ".max.x", g.maxX());
            yml.set(path + ".max.y", g.maxY());
            yml.set(path + ".max.z", g.maxZ());
            yml.set(path + ".zbieranie-do-ekwipunku", g.autoPickup());
            yml.createSection(path + ".bloki");
            for (GeneratorBlock block : g.blocks()) {
                String blockPath = path + ".bloki." + block.material().name();
                yml.set(blockPath + ".waga", block.weight());
                yml.set(blockPath + ".cena", block.price());
                yml.set(blockPath + ".xp", block.xp());
                yml.set(blockPath + ".wypadanie", block.drops());
            }
        }
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie udało się zapisać generators.yml", e);
        }
    }

    public Collection<Generator> all() {
        return Collections.unmodifiableCollection(generators.values());
    }

    public List<String> names() {
        List<String> names = new ArrayList<>();
        for (Generator g : generators.values()) {
            names.add(g.name());
        }
        return names;
    }

    public Generator get(String name) {
        return generators.get(Generator.keyOf(name));
    }

    public boolean exists(Generator generator) {
        return generators.get(generator.key()) == generator;
    }

    public Generator at(Block block) {
        for (Generator g : generators.values()) {
            if (g.contains(block)) {
                return g;
            }
        }
        return null;
    }

    public Generator at(Location loc) {
        if (loc.getWorld() == null) {
            return null;
        }
        String world = loc.getWorld().getName();
        for (Generator g : generators.values()) {
            if (g.contains(world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
                return g;
            }
        }
        return null;
    }

    /** Pierwszy generator nachodzący na podany obszar (z pominięciem {@code except}). */
    public Generator overlapping(String world, int x1, int y1, int z1, int x2, int y2, int z2, Generator except) {
        for (Generator g : generators.values()) {
            if (g != except && g.intersects(world, x1, y1, z1, x2, y2, z2)) {
                return g;
            }
        }
        return null;
    }

    public void add(Generator generator) {
        generators.put(generator.key(), generator);
        save();
    }

    public void remove(Generator generator) {
        generators.remove(generator.key());
        save();
    }
}
