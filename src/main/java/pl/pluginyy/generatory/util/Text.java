package pl.pluginyy.generatory.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Text {

    public static final MiniMessage MM = MiniMessage.miniMessage();

    private Text() {
    }

    /** Tekst do nazw i opisów przedmiotów w GUI (bez domyślnej kursywy). */
    public static Component item(String mini, TagResolver... resolvers) {
        return MM.deserialize("<!i>" + mini, resolvers);
    }

    public static List<Component> lore(String... lines) {
        List<Component> lore = new ArrayList<>(lines.length);
        for (String line : lines) {
            lore.add(item(line));
        }
        return lore;
    }

    /** Nazwa bloku tłumaczona po stronie klienta (np. "Czerwona glazurowana terakota"). */
    public static Component blockName(Material material) {
        return Component.translatable(material.translationKey());
    }

    /** Nazwa bloku jako nazwa przedmiotu w GUI. */
    public static Component itemBlockName(Material material) {
        return blockName(material).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false);
    }

    public static String time(long seconds) {
        if (seconds < 0) {
            seconds = 0;
        }
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) {
            sb.append(h).append("h ");
        }
        if (h > 0 || m > 0) {
            sb.append(m).append("m ");
        }
        sb.append(s).append('s');
        return sb.toString();
    }

    public static String number(double value) {
        return new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(value);
    }

    public static String percent(double value) {
        return new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(value) + "%";
    }

    /** Przyjmuje zarówno "0.5", jak i "0,5". Zwraca null, gdy to nie liczba. */
    public static Double parseNumber(String input) {
        try {
            double value = Double.parseDouble(input.trim().replace(',', '.'));
            return Double.isFinite(value) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
