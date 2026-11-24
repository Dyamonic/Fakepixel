package club.abelix.games.buildffa.game.gui.event;

import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.language.Lang;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

public class StandardEventLeaderboardGui extends Gui {
    private final Consumer<StandardEventLeaderboardGui> renderer;
    private final Set<String> reportedDiagnostics = new HashSet<>();
    private boolean backgroundDrawn;
    private int renderedHeadCount;
    private final List<Integer> renderedLoreRowCounts = new ArrayList<>();
    private final List<LoreTrace> loreTraces = new ArrayList<>();
    private LoreTrace currentLoreTrace;
    private BiConsumer<StandardEventLeaderboardGui, RuntimeException> failureReporter;

    public StandardEventLeaderboardGui(Player player, String eventName, Consumer<StandardEventLeaderboardGui> renderer) {
        super(player, Lang.of("buildffa_event_stats_leaderboard_title").set("%event_name%", eventName), 6);
        this.renderer = renderer;
        draw();
    }

    @Override
    public void draw() {
        if (renderer != null) renderer.accept(this);
    }

    protected String loreKey() {
        return "buildffa_event_stats_player_lore";
    }

    public String playerLoreKey() {
        return loreKey();
    }

    public void prepareBackground(ItemStack filler, Set<Integer> reservedSlots) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Leaderboard inventory operations must run on the server thread");
        if (backgroundDrawn) return;
        for (int slot = 0; slot < 54; slot++) {
            if (!reservedSlots.contains(slot)) add(filler.clone(), slot, event -> event.setCancelled(true));
        }
        backgroundDrawn = true;
    }

    public void refreshLeaderboard() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Leaderboard inventory operations must run on the server thread");
        try {
            draw();
            reopen();
        } catch (RuntimeException exception) {
            if (failureReporter == null) throw exception;
            failureReporter.accept(this, exception);
        }
    }

    public void onFailure(BiConsumer<StandardEventLeaderboardGui, RuntimeException> failureReporter) {
        this.failureReporter = failureReporter;
    }

    public boolean reportDiagnostic(String signature) {
        if (reportedDiagnostics.size() >= 32) reportedDiagnostics.clear();
        return reportedDiagnostics.add(signature);
    }

    public int renderedHeadCount() {
        return renderedHeadCount;
    }

    public List<Integer> renderedLoreRowCounts() {
        return new ArrayList<>(renderedLoreRowCounts);
    }

    public void resetRenderedHeadCount() {
        renderedHeadCount = 0;
        renderedLoreRowCounts.clear();
        loreTraces.clear();
        currentLoreTrace = null;
    }

    protected Map<String, String> statistics(int kills, int collected, int eventsPlayed) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("%kills%", String.valueOf(Math.max(0, kills)));
        values.put("%events_played%", String.valueOf(Math.max(0, eventsPlayed)));
        return values;
    }

    public List<String> playerLore(int kills, int collected, int eventsPlayed) {
        String key = loreKey();
        Map<String, String> values = statistics(kills, collected, eventsPlayed);
        Object[] replacements = new Object[values.size() * 2];
        int index = 0;
        for (Map.Entry<String, String> value : values.entrySet()) {
            replacements[index++] = value.getKey();
            replacements[index++] = value.getValue();
        }
        List<String> rows = Gamer.getGamer(player).getLanguage().getMessageList(key, replacements);
        if (currentLoreTrace == null) beginLoreTrace(0, -1);
        currentLoreTrace.languageClass = Gamer.getGamer(player).getLanguage().getClass().getName();
        currentLoreTrace.listClass = rows == null ? "null" : rows.getClass().getName();
        currentLoreTrace.localization = snapshot(rows);
        currentLoreTrace.localizationCaptured = true;
        if (rows == null || rows.isEmpty())
            throw new IllegalStateException("Missing localization list for " + key);
        List<String> lore = new ArrayList<>(rows.size());
        for (String row : rows) {
            if (row == null || row.equals(key) || row.startsWith("No translation ("))
                throw new IllegalStateException("Missing localization row for " + key);
            String populated = row;
            for (Map.Entry<String, String> value : values.entrySet())
                populated = populated.replace(value.getKey(), value.getValue());
            lore.add(ChatColor.translateAlternateColorCodes('&', populated));
        }
        return lore;
    }

    public void addPlayerItem(ItemStack head, int slot) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Leaderboard inventory operations must run on the server thread");
        ItemMeta sourceMeta = head.getItemMeta();
        List<String> lore = sourceMeta.getLore();
        if (lore == null || lore.isEmpty())
            throw new IllegalStateException("Missing populated leaderboard lore for " + loreKey());
        ItemStack renderedHead = head.clone();
        if (currentLoreTrace != null) {
            currentLoreTrace.beforeAdd = snapshot(renderedHead.getItemMeta().getLore());
            currentLoreTrace.beforeAddCaptured = true;
        }
        add(renderedHead, slot, event -> event.setCancelled(true));
        renderedHeadCount++;
        renderedLoreRowCounts.add(lore.size());
    }
    public void beginLoreTrace(int rank, int slot) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Leaderboard diagnostics must run on the server thread");
        currentLoreTrace = new LoreTrace(rank, slot, loreKey());
        loreTraces.add(currentLoreTrace);
    }

    public void recordDisplayedLore(int slot, List<String> rows) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Displayed lore must be read on the server thread");
        for (LoreTrace trace : loreTraces) {
            if (trace.slot != slot) continue;
            trace.displayed = snapshot(rows);
            trace.displayedCaptured = true;
        }
    }

    public List<String> loreDiagnosticLines() {
        List<String> lines = new ArrayList<>();
        for (LoreTrace trace : loreTraces) {
            String context = "rank=" + trace.rank + " slot=" + trace.slot + " key=" + trace.key;
            lines.add("&7lore_trace &f" + context + " language_class=" + trace.languageClass + " list_class=" + trace.listClass);
            appendRows(lines, context, "getMessageList_return", trace.localization, trace.localizationCaptured);
            appendRows(lines, context, "before_Gui.add", trace.beforeAdd, trace.beforeAddCaptured);
            appendRows(lines, context, "displayed_after_reopen", trace.displayed, trace.displayedCaptured);
            if (trace.localizationCaptured && trace.beforeAddCaptured && trace.displayedCaptured) {
                lines.add("&7lore_comparison &f" + context + " localization_equals_before_add="
                        + java.util.Objects.equals(trace.localization, trace.beforeAdd)
                        + " before_add_equals_displayed=" + java.util.Objects.equals(trace.beforeAdd, trace.displayed));
            }
        }
        return lines;
    }

    private static List<String> snapshot(List<String> rows) {
        return rows == null ? null : new ArrayList<>(rows);
    }

    private static void appendRows(List<String> lines, String context, String stage, List<String> rows, boolean captured) {
        lines.add("&7lore_stage &f" + context + " stage=" + stage + " size="
                + (!captured ? "not_captured" : rows == null ? "null" : String.valueOf(rows.size())));
        if (!captured || rows == null) return;
        for (int index = 0; index < rows.size(); index++) {
            String row = rows.get(index);
            String value = row == null ? "null" : "\"" + row.replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t")
                    .replace("\u00a7", "\\u00a7").replace("&", "\\u0026") + "\"";
            lines.add("&7lore_element &f" + context + " stage=" + stage + " index=" + index + " value=" + value);
        }
    }

    private static final class LoreTrace {
        private final int rank;
        private final int slot;
        private final String key;
        private String languageClass = "not_captured";
        private String listClass = "not_captured";
        private List<String> localization;
        private List<String> beforeAdd;
        private List<String> displayed;
        private boolean localizationCaptured;
        private boolean beforeAddCaptured;
        private boolean displayedCaptured;

        private LoreTrace(int rank, int slot, String key) {
            this.rank = rank;
            this.slot = slot;
            this.key = key;
        }
    }

}
