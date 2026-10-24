package pl.pluginyy.generatory.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class Generator {

    private final String name;
    private String world;
    private int minX, minY, minZ, maxX, maxY, maxZ;
    private boolean autoPickup;
    private final LinkedHashMap<Material, GeneratorBlock> blocks = new LinkedHashMap<>();

    // Tablica losowania budowana leniwie po każdej zmianie bloków
    private Material[] table;
    private double[] cumulative;

    public Generator(String name, String world, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.name = name;
        setArea(world, x1, y1, z1, x2, y2, z2);
    }

    public void setArea(String world, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.world = world;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public String name() {
        return name;
    }

    public String key() {
        return keyOf(name);
    }

    public static String keyOf(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public String worldName() {
        return world;
    }

    public World world() {
        return Bukkit.getWorld(world);
    }

    public int minX() { return minX; }
    public int minY() { return minY; }
    public int minZ() { return minZ; }
    public int maxX() { return maxX; }
    public int maxY() { return maxY; }
    public int maxZ() { return maxZ; }

    public int sizeX() { return maxX - minX + 1; }
    public int sizeY() { return maxY - minY + 1; }
    public int sizeZ() { return maxZ - minZ + 1; }

    public long volume() {
        return (long) sizeX() * sizeY() * sizeZ();
    }

    public String dimensions() {
        return sizeX() + "x" + sizeY() + "x" + sizeZ();
    }

    public boolean autoPickup() {
        return autoPickup;
    }

    public void autoPickup(boolean autoPickup) {
        this.autoPickup = autoPickup;
    }

    public boolean contains(String worldName, int x, int y, int z) {
        return world.equals(worldName)
                && x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public boolean contains(Block block) {
        return contains(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public boolean intersects(String worldName, int x1, int y1, int z1, int x2, int y2, int z2) {
        return world.equals(worldName)
                && Math.min(x1, x2) <= maxX && Math.max(x1, x2) >= minX
                && Math.min(y1, y2) <= maxY && Math.max(y1, y2) >= minY
                && Math.min(z1, z2) <= maxZ && Math.max(z1, z2) >= minZ;
    }

    /** Kwadrat odległości od punktu do najbliższego miejsca w generatorze (0 = w środku). */
    public double distanceSquared(Location loc) {
        double dx = Math.max(Math.max(minX - loc.getX(), 0), loc.getX() - (maxX + 1));
        double dy = Math.max(Math.max(minY - loc.getY(), 0), loc.getY() - (maxY + 1));
        double dz = Math.max(Math.max(minZ - loc.getZ(), 0), loc.getZ() - (maxZ + 1));
        return dx * dx + dy * dy + dz * dz;
    }

    /** Miejsce na środku, na górze generatora. */
    public Location topCenter(World w) {
        return new Location(w, (minX + maxX + 1) / 2.0, maxY + 1, (minZ + maxZ + 1) / 2.0);
    }

    public Collection<GeneratorBlock> blocks() {
        return blocks.values();
    }

    public GeneratorBlock block(Material material) {
        return blocks.get(material);
    }

    public boolean hasBlock(Material material) {
        return blocks.containsKey(material);
    }

    public void addBlock(GeneratorBlock block) {
        blocks.put(block.material(), block);
        invalidate();
    }

    public void removeBlock(Material material) {
        blocks.remove(material);
        invalidate();
    }

    /** Zamienia blok na inny, zachowując jego miejsce na liście i ustawienia. */
    public void replaceBlock(Material from, Material to) {
        LinkedHashMap<Material, GeneratorBlock> copy = new LinkedHashMap<>(blocks);
        blocks.clear();
        for (Map.Entry<Material, GeneratorBlock> entry : copy.entrySet()) {
            if (entry.getKey() == from) {
                blocks.put(to, entry.getValue().withMaterial(to));
            } else {
                blocks.put(entry.getKey(), entry.getValue());
            }
        }
        invalidate();
    }

    public double totalWeight() {
        double total = 0;
        for (GeneratorBlock block : blocks.values()) {
            total += Math.max(0, block.weight());
        }
        return total;
    }

    /** Udział procentowy bloku w generatorze. */
    public double share(GeneratorBlock block) {
        double total = totalWeight();
        return total <= 0 ? 0 : Math.max(0, block.weight()) / total * 100.0;
    }

    public void invalidate() {
        table = null;
        cumulative = null;
    }

    /** Losuje blok zgodnie z proporcjami. Zwraca null, gdy generator nie ma bloków. */
    public Material randomBlock() {
        if (table == null) {
            rebuildTable();
        }
        if (table.length == 0) {
            return null;
        }
        double roll = ThreadLocalRandom.current().nextDouble() * cumulative[cumulative.length - 1];
        for (int i = 0; i < cumulative.length; i++) {
            if (roll < cumulative[i]) {
                return table[i];
            }
        }
        return table[table.length - 1];
    }

    private void rebuildTable() {
        int count = 0;
        for (GeneratorBlock block : blocks.values()) {
            if (block.weight() > 0) {
                count++;
            }
        }
        Material[] newTable = new Material[count];
        double[] newCumulative = new double[count];
        int i = 0;
        double sum = 0;
        for (GeneratorBlock block : blocks.values()) {
            if (block.weight() > 0) {
                sum += block.weight();
                newTable[i] = block.material();
                newCumulative[i] = sum;
                i++;
            }
        }
        cumulative = newCumulative;
        table = newTable;
    }
}
