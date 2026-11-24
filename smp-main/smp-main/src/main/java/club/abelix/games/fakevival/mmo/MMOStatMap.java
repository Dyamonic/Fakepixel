package club.abelix.games.fakevival.mmo;

import java.util.EnumMap;
import java.util.Map;

public final class MMOStatMap {
    private final EnumMap<MMOStatType, Double> values = new EnumMap<>(MMOStatType.class);

    public static MMOStatMap empty() {
        return new MMOStatMap();
    }

    public void set(MMOStatType type, double value) {
        if (type == null || !Double.isFinite(value) || value <= 0.0D) return;
        values.put(type, value);
    }

    public void add(MMOStatType type, double value) {
        if (type == null || !Double.isFinite(value) || value <= 0.0D) return;
        values.merge(type, value, Double::sum);
    }

    public void addAll(MMOStatMap other) {
        if (other == null) return;
        for (Map.Entry<MMOStatType, Double> entry : other.values.entrySet()) add(entry.getKey(), entry.getValue());
    }

    public void remove(MMOStatType type) {
        if (type != null) values.remove(type);
    }

    public double get(MMOStatType type) {
        return values.getOrDefault(type, 0.0D);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Map<MMOStatType, Double> values() {
        return java.util.Collections.unmodifiableMap(values);
    }
}
