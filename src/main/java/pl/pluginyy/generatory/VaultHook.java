package pl.pluginyy.generatory;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import pl.pluginyy.generatory.util.Text;

import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Wypłaty przez Vault. Vault jest opcjonalny, więc łączymy się przez refleksję
 * i nie potrzebujemy go do zbudowania pluginu.
 */
public final class VaultHook {

    private final GeneratoryPlugin plugin;
    private Object economy;
    private Method depositMethod;
    private Method formatMethod;
    private boolean warned;

    public VaultHook(GeneratoryPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean available() {
        resolve();
        return economy != null;
    }

    private void resolve() {
        if (economy != null || Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> provider = Bukkit.getServicesManager().getRegistration(economyClass);
            if (provider == null) {
                return;
            }
            depositMethod = economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);
            formatMethod = economyClass.getMethod("format", double.class);
            economy = provider.getProvider();
        } catch (ReflectiveOperationException | LinkageError e) {
            economy = null;
        }
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (!available()) {
            if (!warned) {
                warned = true;
                plugin.getLogger().warning("Generator ma ustawione ceny, ale na serwerze nie ma Vault "
                        + "z pluginem ekonomii (np. EssentialsX). Pieniądze nie będą wypłacane.");
            }
            return false;
        }
        try {
            depositMethod.invoke(economy, player, amount);
            return true;
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().log(Level.WARNING, "Nie udało się wypłacić pieniędzy przez Vault", e);
            return false;
        }
    }

    public String format(double amount) {
        if (available()) {
            try {
                return String.valueOf(formatMethod.invoke(economy, amount));
            } catch (ReflectiveOperationException ignored) {
                // poniżej zwykły format
            }
        }
        return Text.number(amount) + "$";
    }
}
