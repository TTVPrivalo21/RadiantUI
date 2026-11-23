package dev.privalo.ui.pausemenu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Resolves the player of events whose shape changed between Paper 1.21.x and 26.x
 * (e.g. {@code PlayerLinksSendEvent}), without ever linking against a method that may be missing.
 */
final class EventPlayers {

    private static volatile Boolean configurationPhase;

    private EventPlayers() {
    }

    /** The online player of the event, or null when it is fired before the player exists (26.x configuration phase). */
    static Player resolve(Event event) {
        Object direct = invoke(event, "getPlayer");
        if (direct instanceof Player player) {
            configurationPhase = Boolean.FALSE;
            return player;
        }
        Object connection = invoke(event, "getConnection");
        if (connection != null) {
            configurationPhase = Boolean.TRUE;
            Object profile = invoke(connection, "getProfile");
            Object id = profile == null ? null : invoke(profile, "getId");
            if (id instanceof UUID uuid) {
                return Bukkit.getPlayer(uuid);
            }
        }
        return null;
    }

    /** True once we have seen the 26.x style event (links are sent before the Player object exists). */
    static boolean configurationPhaseLinks() {
        Boolean value = configurationPhase;
        if (value == null) {
            value = hasMethod("org.bukkit.event.player.PlayerLinksSendEvent", "getConnection");
            configurationPhase = value;
        }
        return value;
    }

    /** Calls a public no-arg method looked up on public types only (works with hidden implementation classes). */
    private static Object invoke(Object target, String name) {
        Method method = findPublic(target.getClass(), name);
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(target);
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException ex) {
            return null;
        }
    }

    private static Method findPublic(Class<?> type, String name) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            if (java.lang.reflect.Modifier.isPublic(c.getModifiers())) {
                Method m = noArg(c, name);
                if (m != null) {
                    return m;
                }
            }
            for (Class<?> iface : c.getInterfaces()) {
                Method m = findInInterface(iface, name);
                if (m != null) {
                    return m;
                }
            }
        }
        return null;
    }

    private static Method findInInterface(Class<?> iface, String name) {
        if (java.lang.reflect.Modifier.isPublic(iface.getModifiers())) {
            Method m = noArg(iface, name);
            if (m != null) {
                return m;
            }
        }
        for (Class<?> parent : iface.getInterfaces()) {
            Method m = findInInterface(parent, name);
            if (m != null) {
                return m;
            }
        }
        return null;
    }

    private static Method noArg(Class<?> c, String name) {
        try {
            return c.getMethod(name);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private static boolean hasMethod(String className, String name) {
        try {
            return noArg(Class.forName(className), name) != null;
        } catch (ClassNotFoundException | LinkageError ex) {
            return false;
        }
    }
}
