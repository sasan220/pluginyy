package pl.pluginyy.generatory;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import pl.pluginyy.generatory.model.Generator;
import pl.pluginyy.generatory.model.Selection;

/** Rysuje cząsteczkami obrys zaznaczenia i generatorów graczom trzymającym różdżkę. */
public final class PreviewTask implements Runnable {

    private static final int MAX_POINTS_PER_EDGE = 200;

    private final GeneratoryPlugin plugin;
    private BukkitTask task;
    private double spacing;
    private boolean showGenerators;
    private double rangeSquared;
    private Particle.DustOptions selectionDust;
    private Particle.DustOptions generatorDust;

    public PreviewTask(GeneratoryPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        FileConfiguration cfg = plugin.getConfig();
        spacing = Math.max(0.1, cfg.getDouble("podglad.odstep", 0.5));
        showGenerators = cfg.getBoolean("podglad.pokazuj-generatory", true);
        double range = cfg.getDouble("podglad.zasieg", 96);
        rangeSquared = range * range;
        selectionDust = new Particle.DustOptions(color(cfg.getString("podglad.kolor-zaznaczenia"), 0xFF8FD8), 1.0f);
        generatorDust = new Particle.DustOptions(color(cfg.getString("podglad.kolor-generatorow"), 0x55FF7F), 1.0f);
        long period = Math.max(1, cfg.getLong("podglad.co-ile-tickow", 5));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this, period, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private static Color color(String hex, int fallback) {
        try {
            if (hex != null) {
                return Color.fromRGB(Integer.parseInt(hex.replace("#", "").trim(), 16));
            }
        } catch (IllegalArgumentException ignored) {
            // zły kolor w configu – używamy domyślnego
        }
        return Color.fromRGB(fallback);
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("generatory.admin")
                    || !plugin.wand().isWand(player.getInventory().getItemInMainHand())) {
                continue;
            }
            World world = player.getWorld();

            if (showGenerators) {
                for (Generator g : plugin.generators().all()) {
                    if (g.worldName().equals(world.getName()) && g.distanceSquared(player.getLocation()) <= rangeSquared) {
                        box(player, g.minX(), g.minY(), g.minZ(), g.maxX() + 1, g.maxY() + 1, g.maxZ() + 1, generatorDust);
                    }
                }
            }

            Selection sel = plugin.selections().peek(player);
            if (sel == null) {
                continue;
            }
            if (sel.sameWorld() && world.equals(sel.pos1().getWorld())) {
                Location a = sel.pos1();
                Location b = sel.pos2();
                box(player,
                        Math.min(a.getBlockX(), b.getBlockX()), Math.min(a.getBlockY(), b.getBlockY()), Math.min(a.getBlockZ(), b.getBlockZ()),
                        Math.max(a.getBlockX(), b.getBlockX()) + 1, Math.max(a.getBlockY(), b.getBlockY()) + 1, Math.max(a.getBlockZ(), b.getBlockZ()) + 1,
                        selectionDust);
            } else {
                single(player, sel.pos1());
                single(player, sel.pos2());
            }
        }
    }

    private void single(Player player, Location pos) {
        if (pos != null && player.getWorld().equals(pos.getWorld())) {
            box(player, pos.getBlockX(), pos.getBlockY(), pos.getBlockZ(),
                    pos.getBlockX() + 1, pos.getBlockY() + 1, pos.getBlockZ() + 1, selectionDust);
        }
    }

    private void box(Player p, double x1, double y1, double z1, double x2, double y2, double z2, Particle.DustOptions dust) {
        // 4 krawędzie na dole, 4 na górze i 4 pionowe
        line(p, x1, y1, z1, x2, y1, z1, dust);
        line(p, x1, y1, z2, x2, y1, z2, dust);
        line(p, x1, y1, z1, x1, y1, z2, dust);
        line(p, x2, y1, z1, x2, y1, z2, dust);
        line(p, x1, y2, z1, x2, y2, z1, dust);
        line(p, x1, y2, z2, x2, y2, z2, dust);
        line(p, x1, y2, z1, x1, y2, z2, dust);
        line(p, x2, y2, z1, x2, y2, z2, dust);
        line(p, x1, y1, z1, x1, y2, z1, dust);
        line(p, x2, y1, z1, x2, y2, z1, dust);
        line(p, x1, y1, z2, x1, y2, z2, dust);
        line(p, x2, y1, z2, x2, y2, z2, dust);
    }

    private void line(Player p, double x1, double y1, double z1, double x2, double y2, double z2, Particle.DustOptions dust) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int points = (int) Math.min(MAX_POINTS_PER_EDGE, Math.max(1, Math.ceil(length / spacing)));
        for (int i = 0; i <= points; i++) {
            double t = (double) i / points;
            p.spawnParticle(Particle.DUST, x1 + dx * t, y1 + dy * t, z1 + dz * t, 1, 0, 0, 0, 0, dust);
        }
    }
}
