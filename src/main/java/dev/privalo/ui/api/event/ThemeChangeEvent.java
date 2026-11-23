package dev.privalo.ui.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/** Fired before a player's personal theme changes. {@code newTheme == null} means "back to the global theme". */
public final class ThemeChangeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String oldTheme;
    private final String newTheme;
    private boolean cancelled;

    public ThemeChangeEvent(Player player, String oldTheme, String newTheme) {
        super(player);
        this.oldTheme = oldTheme;
        this.newTheme = newTheme;
    }

    public String getOldTheme() {
        return oldTheme;
    }

    public String getNewTheme() {
        return newTheme;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
