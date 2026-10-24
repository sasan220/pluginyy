package pl.pluginyy.generatory.model;

import org.bukkit.Location;

/** Zaznaczenie gracza zrobione różdżką (dwa narożniki). */
public final class Selection {

    private Location pos1;
    private Location pos2;

    public Location pos1() {
        return pos1;
    }

    public void pos1(Location pos1) {
        this.pos1 = pos1;
    }

    public Location pos2() {
        return pos2;
    }

    public void pos2(Location pos2) {
        this.pos2 = pos2;
    }

    public boolean complete() {
        return pos1 != null && pos2 != null;
    }

    public boolean sameWorld() {
        return complete() && pos1.getWorld() != null && pos1.getWorld().equals(pos2.getWorld());
    }

    public long volume() {
        return (long) (Math.abs(pos1.getBlockX() - pos2.getBlockX()) + 1)
                * (Math.abs(pos1.getBlockY() - pos2.getBlockY()) + 1)
                * (Math.abs(pos1.getBlockZ() - pos2.getBlockZ()) + 1);
    }

    public String dimensions() {
        return (Math.abs(pos1.getBlockX() - pos2.getBlockX()) + 1) + "x"
                + (Math.abs(pos1.getBlockY() - pos2.getBlockY()) + 1) + "x"
                + (Math.abs(pos1.getBlockZ() - pos2.getBlockZ()) + 1);
    }

    public void clear() {
        pos1 = null;
        pos2 = null;
    }
}
