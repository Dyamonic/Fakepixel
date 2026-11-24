package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class EconomyManager {
    private final DataManager dataManager;
    private EconomyAuditManager auditManager;

    public EconomyManager(JavaPlugin plugin, DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public void setAuditManager(EconomyAuditManager auditManager) {
        this.auditManager = auditManager;
    }

    private String path(UUID uuid) {
        return "players." + uuid + ".coins";
    }

    public double getCoins(UUID uuid) {
        return dataManager.getData().getDouble(path(uuid), 0.0D);
    }

    public double getCoins(Player player) {
        return getCoins(player.getUniqueId());
    }

    public void setCoins(UUID uuid, double amount) {
        dataManager.getData().set(path(uuid), Math.max(0.0D, amount));
        dataManager.save();
    }

    public void setCoins(UUID uuid, double amount, String detail) {
        setCoins(uuid, amount);
        log(uuid, "set", amount, detail);
    }

    public void giveCoins(UUID uuid, double amount) {
        giveCoins(uuid, amount, "system");
    }

    public void giveCoins(UUID uuid, double amount, String detail) {
        amount = Math.max(0.0D, amount);
        setCoins(uuid, getCoins(uuid) + amount);
        log(uuid, "give", amount, detail);
    }

    public boolean takeCoins(UUID uuid, double amount) {
        return takeCoins(uuid, amount, "system");
    }

    public boolean takeCoins(UUID uuid, double amount, String detail) {
        amount = Math.max(0.0D, amount);
        if (getCoins(uuid) < amount) return false;
        setCoins(uuid, getCoins(uuid) - amount);
        log(uuid, "take", amount, detail);
        return true;
    }

    public boolean isEcoFrozen(UUID uuid) {
        return auditManager != null && auditManager.isFrozen(uuid);
    }

    public boolean isEcoFrozen(Player player) {
        return player != null && isEcoFrozen(player.getUniqueId());
    }

    private void log(UUID uuid, String action, double amount, String detail) {
        if (auditManager != null) auditManager.logEconomy(uuid, action, amount, getCoins(uuid), detail);
    }

    public String format(double amount) {
        return formatCompact(amount);
    }

    public static String formatCompact(double amount) {
        double absolute = Math.abs(amount);
        if (absolute < 1000.0D) return trim(amount);

        String[] suffixes = {"k", "m", "b", "t"};
        double value = amount;
        int suffixIndex = -1;
        while (Math.abs(value) >= 1000.0D && suffixIndex + 1 < suffixes.length) {
            value /= 1000.0D;
            suffixIndex++;
        }
        return trim(value) + suffixes[suffixIndex];
    }

    private static String trim(double amount) {
        if (amount == (long) amount) return String.valueOf((long) amount);
        String formatted = String.format(java.util.Locale.US, "%.2f", amount);
        while (formatted.contains(".") && formatted.endsWith("0")) formatted = formatted.substring(0, formatted.length() - 1);
        if (formatted.endsWith(".")) formatted = formatted.substring(0, formatted.length() - 1);
        return formatted;
    }

    public List<String> baltop(int limit) {
        List<String> rows = new ArrayList<>();
        org.bukkit.configuration.ConfigurationSection section = dataManager.getData().getConfigurationSection("players");
        if (section == null) return rows;
        List<String> keys = new ArrayList<>(section.getKeys(false));
        keys.sort(Comparator.comparingDouble((String uuid) -> dataManager.getData().getDouble("players." + uuid + ".coins", 0.0D)).reversed());
        int index = 1;
        for (String key : keys) {
            if (index > limit) break;
            double coins = dataManager.getData().getDouble("players." + key + ".coins", 0.0D);
            if (coins <= 0) continue;
            String name = key;
            try {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(UUID.fromString(key));
                if (offline.getName() != null) name = offline.getName();
            } catch (Exception ignored) {
            }
            rows.add(CC.color("&e#" + index + " &f" + name + " &7- &e" + format(coins)));
            index++;
        }
        return rows;
    }
}
