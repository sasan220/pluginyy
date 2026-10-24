package pl.pluginyy.generatory;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import pl.pluginyy.generatory.model.Generator;
import pl.pluginyy.generatory.util.Text;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Odlicza czas do odnowienia, pokazuje pasek i stopniowo podmienia bloki w generatorach. */
public final class RegenManager implements Listener {

    private enum ShowMode { WSZYSCY, SWIAT, W_POBLIZU }

    private final GeneratoryPlugin plugin;
    private final BossBar bar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.GREEN, BossBar.Overlay.NOTCHED_12);
    private final Set<UUID> viewers = new HashSet<>();
    private final ArrayDeque<Job> queue = new ArrayDeque<>();
    private final Set<Generator> queued = new HashSet<>();

    private int interval;
    private int secondsLeft;
    private boolean barEnabled;
    private ShowMode showMode;
    private double showRangeSquared;
    private String barTitle;
    private boolean onlyAir;
    private boolean teleportPlayers;
    private boolean skipUnloaded;
    private int blocksPerTick;
    private String sound;

    private BukkitTask timerTask;
    private BukkitTask workTask;

    public RegenManager(GeneratoryPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        reload();
        secondsLeft = interval;
        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        workTask = Bukkit.getScheduler().runTaskTimer(plugin, this::work, 1L, 1L);
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfig();
        interval = Math.max(5, cfg.getInt("odnawianie.co-ile-sekund", 120));
        onlyAir = cfg.getBoolean("odnawianie.tylko-puste-miejsca", false);
        teleportPlayers = cfg.getBoolean("odnawianie.teleportuj-graczy", true);
        skipUnloaded = cfg.getBoolean("odnawianie.pomijaj-niezaladowane-chunki", true);
        blocksPerTick = Math.max(100, cfg.getInt("odnawianie.blokow-na-tick", 20000));
        sound = cfg.getString("odnawianie.dzwiek", "");

        barEnabled = cfg.getBoolean("bossbar.wlaczony", true);
        showMode = parse(ShowMode.class, cfg.getString("bossbar.pokazuj"), ShowMode.SWIAT);
        double range = cfg.getDouble("bossbar.zasieg", 64);
        showRangeSquared = range * range;
        barTitle = cfg.getString("bossbar.tytul", "<white>Odnowienie za <green><czas>");
        bar.color(parse(BossBar.Color.class, cfg.getString("bossbar.kolor"), BossBar.Color.GREEN));
        bar.overlay(parse(BossBar.Overlay.class, cfg.getString("bossbar.styl"), BossBar.Overlay.NOTCHED_12));

        if (secondsLeft > interval || secondsLeft <= 0) {
            secondsLeft = interval;
        }
        updateBar();
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value, E fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    public void shutdown() {
        if (timerTask != null) {
            timerTask.cancel();
        }
        if (workTask != null) {
            workTask.cancel();
        }
        hideAll();
        queue.clear();
        queued.clear();
    }

    public int interval() {
        return interval;
    }

    public int secondsLeft() {
        return secondsLeft;
    }

    /** Zmienia czas odnawiania i zapisuje go w config.yml. */
    public void setInterval(int seconds) {
        interval = Math.max(5, seconds);
        plugin.getConfig().set("odnawianie.co-ile-sekund", interval);
        plugin.saveConfig();
        secondsLeft = Math.min(secondsLeft, interval);
        updateBar();
    }

    private void tick() {
        if (plugin.generators().all().isEmpty()) {
            secondsLeft = interval;
            hideAll();
            return;
        }
        secondsLeft--;
        if (secondsLeft <= 0) {
            regenerateAll(false);
            secondsLeft = interval;
        }
        updateBar();
    }

    /** Odnawia wszystkie generatory. {@code resetTimer} zaczyna odliczanie od nowa. */
    public void regenerateAll(boolean resetTimer) {
        for (Generator g : plugin.generators().all()) {
            regenerate(g);
        }
        if (resetTimer) {
            secondsLeft = interval;
            updateBar();
        }
        for (UUID id : viewers) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) {
                continue;
            }
            plugin.messages().actionBar(p, "odnowiono-actionbar");
            if (sound != null && !sound.isEmpty()) {
                p.playSound(p.getLocation(), sound, 1f, 1.2f);
            }
        }
    }

    /** Dodaje generator do kolejki odnawiania. Zwraca false, gdy nie ma czego odnawiać. */
    public boolean regenerate(Generator g) {
        if (g.totalWeight() <= 0) {
            return false;
        }
        if (queued.contains(g)) {
            return true;
        }
        World world = g.world();
        if (world == null) {
            return false;
        }
        if (teleportPlayers) {
            for (Player p : world.getPlayers()) {
                Location loc = p.getLocation();
                if (g.contains(world.getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
                    Location target = loc.clone();
                    target.setY(g.maxY() + 1);
                    p.teleport(target);
                }
            }
        }
        queue.add(new Job(g, world));
        queued.add(g);
        return true;
    }

    /** Przerywa trwające odnawianie (np. po usunięciu lub przeniesieniu generatora). */
    public void cancel(Generator g) {
        queue.removeIf(job -> job.generator == g);
        queued.remove(g);
    }

    private void work() {
        int budget = blocksPerTick;
        while (budget > 0 && !queue.isEmpty()) {
            Job job = queue.peek();
            if (!plugin.generators().exists(job.generator)) {
                queue.poll();
                queued.remove(job.generator);
                continue;
            }
            budget -= job.step(budget);
            if (job.done()) {
                queue.poll();
                queued.remove(job.generator);
            }
        }
    }

    private void updateBar() {
        if (!barEnabled || plugin.generators().all().isEmpty()) {
            hideAll();
            return;
        }
        bar.name(Text.MM.deserialize(barTitle, Placeholder.unparsed("czas", Text.time(secondsLeft))));
        bar.progress(Math.max(0f, Math.min(1f, secondsLeft / (float) interval)));
        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean show = shouldSee(p);
            if (show && viewers.add(p.getUniqueId())) {
                p.showBossBar(bar);
            } else if (!show && viewers.remove(p.getUniqueId())) {
                p.hideBossBar(bar);
            }
        }
    }

    private boolean shouldSee(Player p) {
        if (showMode == ShowMode.WSZYSCY) {
            return true;
        }
        String world = p.getWorld().getName();
        Location loc = p.getLocation();
        for (Generator g : plugin.generators().all()) {
            if (!g.worldName().equals(world)) {
                continue;
            }
            if (showMode == ShowMode.SWIAT || g.distanceSquared(loc) <= showRangeSquared) {
                return true;
            }
        }
        return false;
    }

    private void hideAll() {
        for (UUID id : viewers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.hideBossBar(bar);
            }
        }
        viewers.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        viewers.remove(event.getPlayer().getUniqueId());
    }

    /** Odnawianie jednego generatora rozłożone na kilka ticków. */
    private final class Job {

        private final Generator generator;
        private final World world;
        private final int minX, minY, minZ, sizeX, sizeZ;
        private final long total;
        private long index;

        Job(Generator generator, World world) {
            this.generator = generator;
            this.world = world;
            this.minX = generator.minX();
            this.minY = generator.minY();
            this.minZ = generator.minZ();
            this.sizeX = generator.sizeX();
            this.sizeZ = generator.sizeZ();
            this.total = generator.volume();
        }

        int step(int budget) {
            int done = 0;
            long layer = (long) sizeX * sizeZ;
            while (done < budget && index < total) {
                long i = index++;
                done++;
                int y = minY + (int) (i / layer);
                long rest = i % layer;
                int x = minX + (int) (rest % sizeX);
                int z = minZ + (int) (rest / sizeX);
                if (skipUnloaded && !world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }
                Block block = world.getBlockAt(x, y, z);
                if (onlyAir && !block.getType().isAir()) {
                    continue;
                }
                Material material = generator.randomBlock();
                if (material != null && block.getType() != material) {
                    block.setType(material, false);
                }
            }
            return done;
        }

        boolean done() {
            return index >= total;
        }
    }
}
