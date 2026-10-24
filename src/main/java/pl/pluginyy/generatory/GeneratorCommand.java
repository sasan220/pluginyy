package pl.pluginyy.generatory;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.pluginyy.generatory.gui.EditMenu;
import pl.pluginyy.generatory.gui.MainMenu;
import pl.pluginyy.generatory.model.Generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /generator [rozdzka|utworz|usun|edytuj|lista|odnow|tp|przeladuj] */
public final class GeneratorCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of(
            "pomoc", "rozdzka", "utworz", "usun", "edytuj", "lista", "odnow", "tp", "przeladuj");
    private static final List<String> WITH_NAME = List.of("usun", "edytuj", "odnow", "tp");

    private final GeneratoryPlugin plugin;

    public GeneratorCommand(GeneratoryPlugin plugin) {
        this.plugin = plugin;
    }

    /** "Utwórz", "usuń", "różdżka" itd. działają tak samo jak wersje bez polskich znaków. */
    private static String normalize(String input) {
        String s = input.toLowerCase(Locale.ROOT);
        String from = "ąćęłńóśźż";
        String to = "acelnoszz";
        StringBuilder sb = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) {
            int i = from.indexOf(ch);
            sb.append(i >= 0 ? to.charAt(i) : ch);
        }
        return sb.toString();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        Messages msg = plugin.messages();
        Actions actions = plugin.actions();
        if (!sender.hasPermission("generatory.admin")) {
            msg.send(sender, "brak-uprawnien");
            return true;
        }
        if (args.length == 0) {
            if (sender instanceof Player player) {
                new MainMenu(plugin).open(player);
            } else {
                msg.sendList(sender, "pomoc");
            }
            return true;
        }

        String sub = normalize(args[0]);
        String name = args.length > 1 ? args[1] : null;
        Player player = sender instanceof Player p ? p : null;

        switch (sub) {
            case "rozdzka", "wand" -> {
                if (player == null) {
                    msg.send(sender, "tylko-gracz");
                } else {
                    actions.giveWand(player);
                }
            }
            case "utworz", "stworz", "create" -> {
                if (player == null) {
                    msg.send(sender, "tylko-gracz");
                } else if (name == null) {
                    msg.sendList(sender, "pomoc");
                } else {
                    Generator created = actions.create(player, name);
                    if (created != null) {
                        new EditMenu(plugin, created).open(player);
                    }
                }
            }
            case "usun", "delete" -> {
                Generator g = find(sender, name);
                if (g != null) {
                    actions.delete(sender, g);
                }
            }
            case "edytuj", "edit" -> {
                if (player == null) {
                    msg.send(sender, "tylko-gracz");
                    return true;
                }
                Generator g = find(sender, name);
                if (g != null) {
                    new EditMenu(plugin, g).open(player);
                }
            }
            case "lista", "list" -> {
                if (plugin.generators().all().isEmpty()) {
                    msg.send(sender, "lista-pusta");
                    return true;
                }
                msg.send(sender, "lista-naglowek",
                        Placeholder.unparsed("ilosc", String.valueOf(plugin.generators().all().size())));
                for (Generator g : plugin.generators().all()) {
                    msg.send(sender, "lista-pozycja",
                            Placeholder.unparsed("nazwa", g.name()),
                            Placeholder.unparsed("swiat", g.worldName()),
                            Placeholder.unparsed("wymiary", g.dimensions()),
                            Placeholder.unparsed("bloki", String.valueOf(g.volume())));
                }
            }
            case "odnow", "regen" -> {
                if (name == null || normalize(name).equals("wszystkie")) {
                    actions.regenerateAll(sender);
                } else {
                    Generator g = find(sender, name);
                    if (g != null) {
                        actions.regenerate(sender, g);
                    }
                }
            }
            case "tp" -> {
                if (player == null) {
                    msg.send(sender, "tylko-gracz");
                    return true;
                }
                Generator g = find(sender, name);
                if (g != null) {
                    actions.teleport(player, g);
                }
            }
            case "przeladuj", "reload" -> {
                plugin.reload();
                msg.send(sender, "przeladowano");
            }
            default -> msg.sendList(sender, "pomoc");
        }
        return true;
    }

    private Generator find(CommandSender sender, String name) {
        if (name == null) {
            plugin.messages().sendList(sender, "pomoc");
            return null;
        }
        Generator g = plugin.generators().get(name);
        if (g == null) {
            plugin.messages().send(sender, "nie-istnieje", Placeholder.unparsed("nazwa", name));
        }
        return g;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> result = new ArrayList<>();
        if (!sender.hasPermission("generatory.admin")) {
            return result;
        }
        if (args.length == 1) {
            filter(SUBCOMMANDS, args[0], result);
        } else if (args.length == 2 && WITH_NAME.contains(normalize(args[0]))) {
            List<String> options = new ArrayList<>(plugin.generators().names());
            if (normalize(args[0]).equals("odnow")) {
                options.add("wszystkie");
            }
            filter(options, args[1], result);
        }
        return result;
    }

    private static void filter(List<String> options, String prefix, List<String> out) {
        String p = normalize(prefix);
        for (String option : options) {
            if (normalize(option).startsWith(p)) {
                out.add(option);
            }
        }
    }
}
