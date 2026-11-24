package me.outerspace.thepit.util;

import org.bukkit.ChatColor;

import java.text.NumberFormat;
import java.util.Locale;

public final class PitGoldFormatter {
    private PitGoldFormatter() { }

    public static String formatPitGold(long amount) {
        return ChatColor.GOLD + NumberFormat.getIntegerInstance(Locale.US).format(Math.max(0L, amount))
                + " " + ChatColor.GOLD + "Gold";
    }

    public static String number(long amount) {
        return NumberFormat.getIntegerInstance(Locale.US).format(Math.max(0L, amount));
    }
}
