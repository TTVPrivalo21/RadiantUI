package dev.privalo.ui.hook;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Vault economy access via reflection, so Vault stays a pure soft-dependency
 * (no Vault classes are needed at compile time or at runtime).
 */
public final class VaultHook {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private Object economy;
    private Method getBalance;
    private Method format;

    public boolean hook() {
        economy = null;
        getBalance = null;
        format = null;
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            return false;
        }
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(economyClass);
            if (rsp == null) {
                return false;
            }
            economy = rsp.getProvider();
            getBalance = economyClass.getMethod("getBalance", OfflinePlayer.class);
            format = economyClass.getMethod("format", double.class);
            return true;
        } catch (ReflectiveOperationException | LinkageError ex) {
            economy = null;
            return false;
        }
    }

    public boolean available() {
        return economy != null;
    }

    public double balance(OfflinePlayer player) {
        if (economy == null) {
            return 0;
        }
        try {
            return ((Number) getBalance.invoke(economy, player)).doubleValue();
        } catch (ReflectiveOperationException | ClassCastException ex) {
            return 0;
        }
    }

    /** Formatted balance (economy's own format if possible), "-" when no economy is present. */
    public String formatted(OfflinePlayer player) {
        if (economy == null) {
            return "-";
        }
        double value = balance(player);
        try {
            Object result = format.invoke(economy, value);
            if (result instanceof String s && !s.isBlank()) {
                return s;
            }
        } catch (ReflectiveOperationException ex) {
            return FORMAT.format(value);
        }
        return FORMAT.format(value);
    }
}
