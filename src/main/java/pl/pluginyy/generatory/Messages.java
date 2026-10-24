package pl.pluginyy.generatory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.pluginyy.generatory.util.Text;

import java.util.List;

/** Wiadomości z sekcji "wiadomosci" w config.yml (format MiniMessage). */
public final class Messages {

    private final GeneratoryPlugin plugin;
    private Component prefix = Component.empty();

    public Messages(GeneratoryPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        prefix = Text.MM.deserialize(plugin.getConfig().getString("wiadomosci.prefix", ""));
    }

    private String raw(String key) {
        return plugin.getConfig().getString("wiadomosci." + key, "");
    }

    public Component get(String key, TagResolver... resolvers) {
        return parse(raw(key), resolvers);
    }

    private Component parse(String raw, TagResolver... resolvers) {
        return Text.MM.deserialize(raw, TagResolver.resolver(
                TagResolver.resolver(resolvers),
                Placeholder.component("prefix", prefix)));
    }

    /** Wysyła wiadomość. Pusta wiadomość w configu oznacza, że jest wyłączona. */
    public void send(CommandSender to, String key, TagResolver... resolvers) {
        String raw = raw(key);
        if (!raw.isEmpty()) {
            to.sendMessage(parse(raw, resolvers));
        }
    }

    public void actionBar(Player to, String key, TagResolver... resolvers) {
        String raw = raw(key);
        if (!raw.isEmpty()) {
            to.sendActionBar(parse(raw, resolvers));
        }
    }

    public void sendList(CommandSender to, String key) {
        List<String> lines = plugin.getConfig().getStringList("wiadomosci." + key);
        for (String line : lines) {
            to.sendMessage(parse(line));
        }
    }
}
