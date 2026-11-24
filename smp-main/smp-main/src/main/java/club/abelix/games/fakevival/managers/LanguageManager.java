package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class LanguageManager {
    private final JavaPlugin plugin;
    private final DataManager dataManager;

    public LanguageManager(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    public String getLanguage(Player player) {
        if (player == null) return getDefaultLanguage();
        String selected = dataManager.getData().getString(path(player), getDefaultLanguage());
        return isLanguage(selected) ? selected.toLowerCase() : getDefaultLanguage();
    }

    public void setLanguage(Player player, String language) {
        if (player == null || !isLanguage(language)) return;
        dataManager.getData().set(path(player), language.toLowerCase());
        dataManager.save();
    }

    public boolean isLanguage(String language) {
        return language != null && club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).isConfigurationSection(languagePath(language));
    }

    public List<String> languages() {
        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("languages.available");
        if (section == null) return List.of(getDefaultLanguage());
        return new ArrayList<>(section.getKeys(false));
    }

    public String displayName(String language) {
        return CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(languagePath(language) + ".display", language));
    }

    public String material(String language) {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(languagePath(language) + ".icon", "PAPER");
    }

    public String message(Player player, String key) {
        String language = getLanguage(player);
        String fallback = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("languages.available." + getDefaultLanguage() + ".messages." + key, key);
        return CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(languagePath(language) + ".messages." + key, fallback));
    }

    public List<String> lore(Player player, String key) {
        String language = getLanguage(player);
        List<String> fallback = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("languages.available." + getDefaultLanguage() + ".messages." + key);
        List<String> lines = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(languagePath(language) + ".messages." + key);
        return CC.colorList(lines.isEmpty() ? fallback : lines);
    }

    public String getDefaultLanguage() {
        String fallback = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("languages.default", "en").toLowerCase();
        return isLanguage(fallback) ? fallback : "en";
    }

    private String languagePath(String language) {
        return "languages.available." + language.toLowerCase();
    }

    private String path(Player player) {
        return "players." + player.getUniqueId() + ".settings.language";
    }
}
