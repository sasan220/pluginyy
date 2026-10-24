package pl.pluginyy.generatory.model;

import org.bukkit.Material;

/** Jeden rodzaj bloku w generatorze: jego proporcja (waga), cena i nagrody. */
public final class GeneratorBlock {

    private final Material material;
    private double weight;
    private double price;
    private int xp;
    private boolean drops;

    public GeneratorBlock(Material material, double weight, double price, int xp, boolean drops) {
        this.material = material;
        this.weight = weight;
        this.price = price;
        this.xp = xp;
        this.drops = drops;
    }

    public GeneratorBlock withMaterial(Material other) {
        return new GeneratorBlock(other, weight, price, xp, drops);
    }

    public Material material() {
        return material;
    }

    public double weight() {
        return weight;
    }

    public void weight(double weight) {
        this.weight = weight;
    }

    public double price() {
        return price;
    }

    public void price(double price) {
        this.price = price;
    }

    public int xp() {
        return xp;
    }

    public void xp(int xp) {
        this.xp = xp;
    }

    public boolean drops() {
        return drops;
    }

    public void drops(boolean drops) {
        this.drops = drops;
    }
}
