package club.abelix.games.fakevival.furniture;

import com.google.gson.*;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SmpModelImporter {
    private static final int MAX_JSON_CHARS = 1_000_000;
    private static final int MAX_IMPORTED_PARTS = 80;

    public SmpFurnitureModel parse(String requestedId, String json) {
        return parse(requestedId, json, null, FurnitureDefinition.Category.FURNITURE);
    }

    public SmpFurnitureModel parse(String requestedId, String json, String modelMeta, FurnitureDefinition.Category fallbackCategory) {
        if (json == null || json.length() > MAX_JSON_CHARS) throw new IllegalArgumentException("model file is empty or too large");
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        String id = clean(requestedId);
        if (id.isBlank()) throw new IllegalArgumentException("invalid model id");

        ModelMeta meta = parseMeta(modelMeta);
        String name = meta.name().isBlank() ? string(root, "name", pretty(id)) : meta.name();
        FurnitureDefinition.Category category = FurnitureDefinition.Category.parse(string(root, "category", fallbackCategory == null ? "Furniture" : fallbackCategory.name()));
        double price = number(root, "price", defaultPrice(category));

        List<SmpModelPart> parts = root.has("parts") ? customParts(root.getAsJsonArray("parts")) : blockbenchParts(root, meta.partMaterials());
        if (parts.isEmpty()) throw new IllegalArgumentException("no model parts found");
        if (parts.size() > MAX_IMPORTED_PARTS) throw new IllegalArgumentException("model exceeds the " + MAX_IMPORTED_PARTS + " part limit");

        Material preview = meta.previewMaterial() == null ? parts.getFirst().material() : meta.previewMaterial();
        Material configured = Material.matchMaterial(string(root, "preview_material", preview.name()));
        if (configured != null) preview = configured;
        double width = number(root, "hitbox_width", estimateWidth(root));
        double height = number(root, "hitbox_height", estimateHeight(root));
        return new SmpFurnitureModel(id, name, category, preview, Math.max(0, price), parts, width, height, null, null);
    }

    private List<SmpModelPart> customParts(JsonArray array) {
        List<SmpModelPart> out = new ArrayList<>();
        for (JsonElement value : array) {
            if (!value.isJsonObject()) continue;
            JsonObject p = value.getAsJsonObject();
            Material material = material(p, Map.of());
            SmpModelPart.EntityType type = enumValue(SmpModelPart.EntityType.class, string(p, "type", material.isBlock() ? "BLOCK_DISPLAY" : "ITEM_DISPLAY"), SmpModelPart.EntityType.BLOCK_DISPLAY);
            EquipmentSlot slot = enumValue(EquipmentSlot.class, string(p, "slot", "HEAD"), EquipmentSlot.HEAD);
            double[] offset = vector(p, "offset", 0);
            double[] rotation = vector(p, "rotation", 0);
            double[] scaleVector = scaleVector(p, number(p, "scale", 1));
            out.add(new SmpModelPart(material, type, slot, offset[0], offset[1], offset[2], (float) rotation[0], (float) rotation[1], (float) rotation[2],
                    (float) Math.max(scaleVector[0], Math.max(scaleVector[1], scaleVector[2])), (float) scaleVector[0], (float) scaleVector[1], (float) scaleVector[2],
                    bool(p, "visible", true), bool(p, "small", false), bool(p, "marker", true), bool(p, "gravity", false), bool(p, "baseplate", false), bool(p, "arms", false)));
        }
        return out;
    }

    private List<SmpModelPart> blockbenchParts(JsonObject root, Map<String, Material> partMaterials) {
        List<SmpModelPart> out = new ArrayList<>();
        JsonArray elements = root.has("elements") && root.get("elements").isJsonArray() ? root.getAsJsonArray("elements") : new JsonArray();
        for (JsonElement value : elements) {
            if (!value.isJsonObject()) continue;
            JsonObject e = value.getAsJsonObject();
            Material material = material(e, partMaterials);
            double[] from = vector(e, "from", 0), to = vector(e, "to", 16);
            double x = (Math.min(from[0], to[0]) - 8) / 16d;
            double y = Math.min(from[1], to[1]) / 16d;
            double z = (Math.min(from[2], to[2]) - 8) / 16d;
            float yaw = 0, pitch = 0, roll = 0;
            if (e.has("rotation")) {
                JsonElement rot = e.get("rotation");
                if (rot.isJsonObject()) {
                    JsonObject r = rot.getAsJsonObject();
                    float angle = (float) number(r, "angle", 0);
                    String axis = string(r, "axis", "y");
                    if (axis.equalsIgnoreCase("y")) yaw = angle;
                    else if (axis.equalsIgnoreCase("x")) pitch = angle;
                    else roll = angle;
                } else if (rot.isJsonArray()) {
                    JsonArray r = rot.getAsJsonArray();
                    if (r.size() > 0) pitch = safeFloat(r.get(0));
                    if (r.size() > 1) yaw = safeFloat(r.get(1));
                    if (r.size() > 2) roll = safeFloat(r.get(2));
                }
            }
            double sx = Math.abs(to[0] - from[0]) / 16d;
            double sy = Math.abs(to[1] - from[1]) / 16d;
            double sz = Math.abs(to[2] - from[2]) / 16d;
            float scale = (float) Math.clamp(Math.max(Math.max(sx, sy), sz), .01, 8);
            out.add(new SmpModelPart(material, material.isBlock() ? SmpModelPart.EntityType.BLOCK_DISPLAY : SmpModelPart.EntityType.ITEM_DISPLAY,
                    EquipmentSlot.HEAD, x, y, z, yaw, pitch, roll, scale, (float) sx, (float) sy, (float) sz,
                    true, false, true, false, false, false));
        }
        return out;
    }

    private Material material(JsonObject p, Map<String, Material> partMaterials) {
        String elementName = string(p, "name", "");
        Material mapped = partMaterials.get(cleanKey(elementName));
        if (mapped != null) return mapped;
        String raw = string(p, "material", elementName.isBlank() ? "OAK_PLANKS" : elementName).toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        Material value = Material.matchMaterial(raw);
        if (value != null) return value;
        // Common Blockbench part names from furniture packs.
        String key = cleanKey(elementName);
        if (key.contains("wood") || key.contains("leg") || key.contains("frame")) return Material.OAK_PLANKS;
        if (key.contains("glass")) return Material.GLASS;
        if (key.contains("lamp") || key.contains("light")) return Material.LANTERN;
        if (key.contains("leaf") || key.contains("plant")) return Material.OAK_LEAVES;
        if (key.contains("pillow") || key.contains("cushion") || key.contains("sheet")) return Material.WHITE_WOOL;
        if (key.contains("stone") || key.contains("quartz")) return Material.SMOOTH_QUARTZ;
        if (key.contains("metal") || key.contains("iron")) return Material.IRON_BLOCK;
        return Material.OAK_PLANKS;
    }

    private ModelMeta parseMeta(String metaText) {
        if (metaText == null || metaText.isBlank()) return new ModelMeta("", null, Map.of());
        String name = first(metaText, "(?m)^name:\\s*\"?([^\"\\r\\n]+)\"?\\s*$");
        Material preview = null;
        Matcher itemType = Pattern.compile("(?m)^\\s{4}type:\\s*([A-Za-z0-9_<>%-]+)\\s*$").matcher(metaText);
        if (itemType.find()) preview = materialFromTemplate(itemType.group(1), Map.of());
        Map<String, String> defaults = parseParameterDefaults(metaText);
        Map<String, Material> partMaterials = parsePartMaterials(metaText, defaults);
        return new ModelMeta(name == null ? "" : name, preview, partMaterials);
    }

    private Map<String, String> parseParameterDefaults(String text) {
        Map<String, String> values = new HashMap<>();
        String current = null;
        for (String line : text.split("\\R")) {
            Matcher key = Pattern.compile("^\\s{2}([A-Za-z0-9_-]+):\\s*$").matcher(line);
            if (key.find()) current = key.group(1);
            Matcher def = Pattern.compile("^\\s{4}default:\\s*([A-Za-z0-9_%-]+)\\s*$").matcher(line);
            if (current != null && def.find()) values.put(current.toLowerCase(Locale.ROOT), def.group(1).toUpperCase(Locale.ROOT));
        }
        return values;
    }

    private Map<String, Material> parsePartMaterials(String text, Map<String, String> defaults) {
        Map<String, Material> values = new HashMap<>();
        boolean inParts = false;
        String current = null;
        for (String line : text.split("\\R")) {
            if (line.trim().equals("parts:")) { inParts = true; continue; }
            if (!inParts) continue;
            Matcher part = Pattern.compile("^\\s{2}([A-Za-z0-9_-]+):\\s*(?:#.*)?$").matcher(line);
            if (part.find()) { current = part.group(1); continue; }
            Matcher mat = Pattern.compile("^\\s{4}material:\\s*([A-Za-z0-9_<>%-]+)\\s*(?:#.*)?$").matcher(line);
            if (current != null && mat.find()) {
                Material material = materialFromTemplate(mat.group(1), defaults);
                if (material != null) values.put(cleanKey(current), material);
            }
        }
        return values;
    }

    private Material materialFromTemplate(String input, Map<String, String> defaults) {
        if (input == null) return null;
        String value = input.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        Matcher placeholder = Pattern.compile("<([A-Z0-9_%-]+)>").matcher(value);
        StringBuffer out = new StringBuffer();
        while (placeholder.find()) {
            String replacement = defaults.getOrDefault(placeholder.group(1).toLowerCase(Locale.ROOT), "WHITE");
            placeholder.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        placeholder.appendTail(out);
        Material material = Material.matchMaterial(out.toString());
        return material == null || material.isAir() ? null : material;
    }

    private double[] scaleVector(JsonObject p, double fallback) {
        double[] v = {fallback, fallback, fallback};
        if (p.has("scale") && p.get("scale").isJsonArray()) {
            JsonArray a = p.getAsJsonArray("scale");
            for (int i = 0; i < Math.min(3, a.size()); i++) try { v[i] = a.get(i).getAsDouble(); } catch (Exception ignored) {}
        }
        if (p.has("scaleX")) v[0] = number(p, "scaleX", v[0]);
        if (p.has("scaleY")) v[1] = number(p, "scaleY", v[1]);
        if (p.has("scaleZ")) v[2] = number(p, "scaleZ", v[2]);
        return v;
    }

    private double estimateWidth(JsonObject root) {
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY, minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        JsonArray elements = root.has("elements") && root.get("elements").isJsonArray() ? root.getAsJsonArray("elements") : new JsonArray();
        for (JsonElement value : elements) if (value.isJsonObject()) {
            JsonObject e = value.getAsJsonObject(); double[] from = vector(e, "from", 0), to = vector(e, "to", 16);
            minX = Math.min(minX, Math.min(from[0], to[0])); maxX = Math.max(maxX, Math.max(from[0], to[0]));
            minZ = Math.min(minZ, Math.min(from[2], to[2])); maxZ = Math.max(maxZ, Math.max(from[2], to[2]));
        }
        if (!Double.isFinite(minX)) return 1;
        return Math.max(.25, Math.max(maxX - minX, maxZ - minZ) / 16d);
    }

    private double estimateHeight(JsonObject root) {
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        JsonArray elements = root.has("elements") && root.get("elements").isJsonArray() ? root.getAsJsonArray("elements") : new JsonArray();
        for (JsonElement value : elements) if (value.isJsonObject()) {
            JsonObject e = value.getAsJsonObject(); double[] from = vector(e, "from", 0), to = vector(e, "to", 16);
            minY = Math.min(minY, Math.min(from[1], to[1])); maxY = Math.max(maxY, Math.max(from[1], to[1]));
        }
        if (!Double.isFinite(minY)) return 1;
        return Math.max(.25, (maxY - minY) / 16d);
    }

    private double[] vector(JsonObject o, String key, double fallback) {
        double[] v = {fallback, fallback, fallback};
        if (o.has(key) && o.get(key).isJsonArray()) {
            JsonArray a = o.getAsJsonArray(key);
            for (int i = 0; i < Math.min(3, a.size()); i++) try { v[i] = a.get(i).getAsDouble(); } catch (Exception ignored) {}
        }
        return v;
    }

    private static float safeFloat(JsonElement value) { try { return value.getAsFloat(); } catch (Exception ignored) { return 0; } }
    private static String string(JsonObject o, String k, String d) { try { return o.has(k) ? o.get(k).getAsString() : d; } catch (Exception ignored) { return d; } }
    private static double number(JsonObject o, String k, double d) { try { return o.has(k) ? o.get(k).getAsDouble() : d; } catch (Exception ignored) { return d; } }
    private static boolean bool(JsonObject o, String k, boolean d) { try { return o.has(k) ? o.get(k).getAsBoolean() : d; } catch (Exception ignored) { return d; } }
    private static <T extends Enum<T>> T enumValue(Class<T> type, String value, T fallback) { try { return Enum.valueOf(type, value.toUpperCase(Locale.ROOT)); } catch (Exception ignored) { return fallback; } }
    private static String clean(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_").replaceAll("_+", "_").replaceAll("^_|_$", ""); }
    private static String cleanKey(String value) { return clean(value).toLowerCase(Locale.ROOT); }
    private static String pretty(String id) { String text = id.replace('_', ' '); return text.isBlank() ? "Furniture" : Character.toUpperCase(text.charAt(0)) + text.substring(1); }
    private static String first(String text, String pattern) { Matcher matcher = Pattern.compile(pattern).matcher(text); return matcher.find() ? matcher.group(1).trim() : null; }
    private static double defaultPrice(FurnitureDefinition.Category category) { return switch (category) { case CHAIRS -> 175; case TABLES -> 250; case BEDS -> 400; case LAMPS -> 150; case OUTDOOR -> 220; case DECORATIONS -> 200; default -> 200; }; }
    private record ModelMeta(String name, Material previewMaterial, Map<String, Material> partMaterials) {}
}
