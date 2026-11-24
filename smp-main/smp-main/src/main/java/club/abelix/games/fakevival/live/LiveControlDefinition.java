package club.abelix.games.fakevival.live;

import java.util.Locale;

public record LiveControlDefinition(String key, LiveControlType type, String category, String defaultValue, boolean major) {
    public boolean defaultBoolean() {
        return Boolean.parseBoolean(defaultValue);
    }

    public double defaultDouble() {
        try {
            return Double.parseDouble(defaultValue);
        } catch (NumberFormatException ignored) {
            return 0.0D;
        }
    }

    public String label() {
        String normalized = key.replace('.', ' ').replace('_', ' ').replace('-', ' ');
        if (normalized.isBlank()) return key;
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }
}
