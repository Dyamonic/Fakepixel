package me.outerspace.thepit.util;

import org.bukkit.ChatColor;

public final class CC {
    private CC() {}
    public static String color(String s) { return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s); }
}
