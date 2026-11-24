package club.abelix.games.fakevival.util;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.LongAdder;

/** Removes internal vendor-registry descriptions without using lore for item identity or routing. */
public final class VendorLoreSanitizer {
    private VendorLoreSanitizer() {}

    private static final List<String> BLOCKED = List.of(
            "themed stock from",
            "java registry vendor item",
            "placeholder vendor item",
            "generated shop item");
    private static final LongAdder PREVIEW_ITEMS_SANITIZED = new LongAdder();
    private static final LongAdder PURCHASED_ITEMS_SANITIZED = new LongAdder();

    public enum Context { GENERAL, SHOP_PREVIEW, PURCHASED_ITEM }

    public static List<String> sanitizeVendorLore(List<String> lore) {
        if (lore == null || lore.isEmpty()) return lore == null ? List.of() : new ArrayList<>(lore);
        List<String> cleaned = new ArrayList<>(lore.size());
        for (String line : lore) if (line != null && !isBlockedLine(line)) cleaned.add(line);
        return cleaned;
    }

    public static boolean sanitizeItem(ItemStack item, Context context) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore() || meta.getLore() == null) return false;
        List<String> original = meta.getLore();
        List<String> cleaned = sanitizeVendorLore(original);
        if (cleaned.size() == original.size()) return false;
        meta.setLore(cleaned);
        item.setItemMeta(meta);
        if (context == Context.SHOP_PREVIEW) PREVIEW_ITEMS_SANITIZED.increment();
        if (context == Context.PURCHASED_ITEM) PURCHASED_ITEMS_SANITIZED.increment();
        return true;
    }

    public static boolean containsBlockedLore(List<String> lore) {
        return lore != null && lore.stream().anyMatch(VendorLoreSanitizer::isBlockedLine);
    }

    public static boolean containsPhrase(List<String> lore, String phrase) {
        if (lore == null || phrase == null || phrase.isBlank()) return false;
        String expected = phrase.toLowerCase(Locale.ROOT);
        return lore.stream().map(VendorLoreSanitizer::plain).anyMatch(line -> line.contains(expected));
    }

    public static Metrics metrics() {
        return new Metrics(PREVIEW_ITEMS_SANITIZED.sum(), PURCHASED_ITEMS_SANITIZED.sum());
    }

    private static boolean isBlockedLine(String line) {
        String plain = plain(line);
        return BLOCKED.stream().anyMatch(plain::contains);
    }

    private static String plain(String line) {
        if (line == null) return "";
        String stripped = ChatColor.stripColor(CC.color(line));
        return (stripped == null ? "" : stripped).toLowerCase(Locale.ROOT);
    }

    public record Metrics(long previewItemsSanitized, long purchasedItemsSanitized) {}
}
