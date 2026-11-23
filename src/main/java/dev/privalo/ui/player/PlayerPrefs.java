package dev.privalo.ui.player;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Per-player preferences stored in the player's PersistentDataContainer
 * (saved with the player file, no database needed).
 */
public final class PlayerPrefs {

    private final NamespacedKey themeKey;
    private final NamespacedKey hudKey;
    private final NamespacedKey scoreboardKey;

    public PlayerPrefs(Plugin plugin) {
        this.themeKey = new NamespacedKey(plugin, "theme");
        this.hudKey = new NamespacedKey(plugin, "hud");
        this.scoreboardKey = new NamespacedKey(plugin, "scoreboard");
    }

    public String theme(Player player) {
        return player.getPersistentDataContainer().get(themeKey, PersistentDataType.STRING);
    }

    public void theme(Player player, String themeId) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (themeId == null) {
            pdc.remove(themeKey);
        } else {
            pdc.set(themeKey, PersistentDataType.STRING, themeId);
        }
    }

    public boolean hud(Player player, boolean def) {
        return flag(player, hudKey, def);
    }

    public void setHud(Player player, boolean value) {
        player.getPersistentDataContainer().set(hudKey, PersistentDataType.BYTE, (byte) (value ? 1 : 0));
    }

    public boolean scoreboard(Player player, boolean def) {
        return flag(player, scoreboardKey, def);
    }

    public void setScoreboard(Player player, boolean value) {
        player.getPersistentDataContainer().set(scoreboardKey, PersistentDataType.BYTE, (byte) (value ? 1 : 0));
    }

    private boolean flag(Player player, NamespacedKey key, boolean def) {
        Byte value = player.getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return value == null ? def : value != 0;
    }
}
