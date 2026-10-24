package pl.pluginyy.generatory;

import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import pl.pluginyy.generatory.gui.Menu;
import pl.pluginyy.generatory.listener.GeneratorListener;
import pl.pluginyy.generatory.listener.MenuListener;
import pl.pluginyy.generatory.listener.WandListener;

public final class GeneratoryPlugin extends JavaPlugin {

    private Messages messages;
    private Wand wand;
    private SelectionManager selections;
    private ChatInput chatInput;
    private VaultHook vault;
    private GeneratorManager generators;
    private RegenManager regen;
    private PreviewTask preview;
    private Actions actions;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        messages = new Messages(this);
        wand = new Wand(this);
        selections = new SelectionManager();
        chatInput = new ChatInput(this);
        vault = new VaultHook(this);
        actions = new Actions(this);
        generators = new GeneratorManager(this);
        generators.load();
        regen = new RegenManager(this);
        regen.start();
        preview = new PreviewTask(this);
        preview.start();

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(selections, this);
        pm.registerEvents(chatInput, this);
        pm.registerEvents(regen, this);
        pm.registerEvents(new WandListener(this), this);
        pm.registerEvents(new GeneratorListener(this), this);
        pm.registerEvents(new MenuListener(this), this);

        PluginCommand command = getCommand("generator");
        if (command != null) {
            GeneratorCommand executor = new GeneratorCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    @Override
    public void onDisable() {
        closeMenus();
        if (preview != null) {
            preview.stop();
        }
        if (regen != null) {
            regen.shutdown();
        }
        if (generators != null) {
            generators.save();
        }
    }

    public void reload() {
        closeMenus();
        reloadConfig();
        messages.reload();
        generators.load();
        regen.reload();
        preview.start();
    }

    private void closeMenus() {
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof Menu) {
                player.closeInventory();
            }
        }
    }

    public Messages messages() {
        return messages;
    }

    public Wand wand() {
        return wand;
    }

    public SelectionManager selections() {
        return selections;
    }

    public ChatInput chatInput() {
        return chatInput;
    }

    public VaultHook vault() {
        return vault;
    }

    public GeneratorManager generators() {
        return generators;
    }

    public RegenManager regen() {
        return regen;
    }

    public Actions actions() {
        return actions;
    }
}
