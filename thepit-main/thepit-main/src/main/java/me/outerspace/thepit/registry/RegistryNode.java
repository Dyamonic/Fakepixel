package me.outerspace.thepit.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RegistryNode {
    private final Map<String, Object> values;

    protected RegistryNode(Map<String, Object> values) {
        this.values = values == null ? Collections.<String, Object>emptyMap() : values;
    }

    public static RegistryNode empty() { return new RegistryNode(Collections.emptyMap()); }

    public Optional<Object> value(String path) { return Optional.ofNullable(resolve(path)); }
    public boolean contains(String path) { return resolve(path) != null; }
    public String getString(String path) { return getString(path, null); }
    public String getString(String path, String fallback) {
        Object value = resolve(path);
        return value == null ? fallback : String.valueOf(value);
    }
    public int getInt(String path) { return getInt(path, 0); }
    public int getInt(String path, int fallback) {
        Object value = resolve(path);
        if (value instanceof Number) return ((Number) value).intValue();
        try { return value == null ? fallback : Integer.parseInt(String.valueOf(value)); } catch (Exception ignored) { return fallback; }
    }
    public long getLong(String path) { return getLong(path, 0L); }
    public long getLong(String path, long fallback) {
        Object value = resolve(path);
        if (value instanceof Number) return ((Number) value).longValue();
        try { return value == null ? fallback : Long.parseLong(String.valueOf(value)); } catch (Exception ignored) { return fallback; }
    }
    public double getDouble(String path) { return getDouble(path, 0.0D); }
    public double getDouble(String path, double fallback) {
        Object value = resolve(path);
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return value == null ? fallback : Double.parseDouble(String.valueOf(value)); } catch (Exception ignored) { return fallback; }
    }
    public boolean getBoolean(String path) { return getBoolean(path, false); }
    public boolean getBoolean(String path, boolean fallback) {
        Object value = resolve(path);
        return value instanceof Boolean ? (Boolean) value : value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }
    public List<String> getStringList(String path) {
        Object value = resolve(path);
        if (!(value instanceof List)) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (Object item : (List<?>) value) out.add(String.valueOf(item));
        return Collections.unmodifiableList(out);
    }
    public List<Integer> getIntegerList(String path) {
        Object value = resolve(path);
        if (!(value instanceof List)) return Collections.emptyList();
        List<Integer> out = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (item instanceof Number) out.add(((Number) item).intValue());
            else try { out.add(Integer.parseInt(String.valueOf(item))); } catch (Exception ignored) { }
        }
        return Collections.unmodifiableList(out);
    }
    public Optional<RegistryNode> section(String path) {
        Object value = resolve(path);
        if (!(value instanceof Map)) return Optional.empty();
        @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) value;
        return Optional.of(new RegistryNode(map));
    }
    public Set<String> keys(String path) {
        Optional<RegistryNode> section = section(path);
        return section.isPresent() ? section.get().keys() : Collections.<String>emptySet();
    }
    public Set<String> keys() { return Collections.unmodifiableSet(new LinkedHashSet<>(values.keySet())); }
    public RegistryNode getConfigurationSection(String path) { return section(path).orElse(null); }
    public Set<String> getKeys(boolean deep) { return keys(); }

    private Object resolve(String path) {
        if (path == null || path.isEmpty()) return values;
        Object current = values;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map)) return null;
            current = ((Map<?, ?>) current).get(part);
            if (current == null) return null;
        }
        return current;
    }
}
