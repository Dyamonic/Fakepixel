package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.events.PitLevelChangeEvent;
import me.outerspace.thepit.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class LevelUpFeedbackManager implements Listener {
    private final ThePitPlugin plugin;
    private final Set<String> warnings = new HashSet<>();
    private Sound sound;
    private Effect particle;
    private TitlePackets titlePackets;
    private boolean titlePacketsFailed;
    private final Map<UUID, Long> titleBusyUntil = new HashMap<>();

    public LevelUpFeedbackManager(ThePitPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        warnings.clear();
        sound = parseSound(plugin.registry().settings().getString("level-up.sound.name", "LEVEL_UP"));
        particle = parseParticle(plugin.registry().settings().getString("level-up.particles.name", "HAPPY_VILLAGER"));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        titleBusyUntil.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevelChange(PitLevelChangeEvent event) {
        if (event.getNewLevel() == event.getOldLevel()) return;
        Player player = Bukkit.getPlayer(event.getPlayerUuid());
        if (player == null || !player.isOnline()) return;
        String source = event.getSource() == null ? "" : event.getSource().toUpperCase(Locale.ENGLISH);
        int authoritativeLevel = plugin.getStatsManager().getLevel(event.getPlayerUuid());
        int authoritativePrestige = plugin.getStatsManager().getPrestige(event.getPlayerUuid());
        if (plugin.registry().tablist().debugEnabled()) plugin.getLogger().info("Progression display refresh player=" + player.getName()
                + " previous_level=" + event.getOldLevel() + " new_level=" + event.getNewLevel()
                + " authoritative_level=" + authoritativeLevel + " prestige=" + authoritativePrestige
                + " source=" + source);
        refresh(player, "LEVEL_CHANGED:" + source);
        if (event.getNewLevel() <= event.getOldLevel() || event.isPrestigeReset()) return;
        if (source.equals("LOAD") || source.equals("MIGRATION") || source.equals("SET_LEVEL") || source.equals("PRESTIGE_RESET")) return;
        if (event.isAdministrative() && !source.equals("ADMINISTRATIVE")) return;
        String oldFormatted = plugin.getStatsManager().getFormattedLevel(event.getPlayerUuid(), event.getOldLevel());
        String newFormatted = plugin.getStatsManager().getFormattedLevel(event.getPlayerUuid(), event.getNewLevel());
        if (plugin.registry().settings().getBoolean("level-up.title.enabled", true)) {
            String title = replace(plugin.registry().settings().getString("level-up.title.title", "&b&lLEVEL UP!"), oldFormatted, newFormatted);
            String subtitle = replace(plugin.registry().settings().getString("level-up.title.subtitle", "%old_level_formatted% &b➜ %new_level_formatted%"), oldFormatted, newFormatted);
            sendTitle(player, title, subtitle);
        }
        playSound(player);
        playParticles(player);
        if (event.getNewLevel() >= 120 && event.getOldLevel() < 120) {
            player.sendMessage(CC.color(plugin.registry().settings().getString("level-up.maximum-level-message", "&b&lMAX LEVEL! &7You reached level &c120&7 and can now prestige.")));
        }
        if (event.isStatisticsUnlocked()) {
            player.sendMessage(CC.color(plugin.registry().settings().getString("level-up.statistics-unlock.title", "&a&lFEATURE UNLOCKED!")));
            player.sendMessage(CC.color(plugin.registry().settings().getString("level-up.statistics-unlock.message", "&7You permanently unlocked the Statistics menu.")));
        }
        int featureTitles = queueFeatureUnlocks(player, event);
        int queuedTitles = queuePerkSlotUnlocks(player, event.getOldLevel(), event.getNewLevel(), featureTitles);
        long stayMillis = Math.max(1L, plugin.registry().settings().getInt("level-up.title.stay-ticks", 40)) * 50L;
        if (queuedTitles > 0) {
            long delay = Math.max(1L, plugin.registry().settings().getLong("level-up.perk-slot-unlock.delay-ticks", 65L));
            long spacing = Math.max(1L, plugin.registry().settings().getLong("level-up.perk-slot-unlock.spacing-ticks", 55L));
            titleBusyUntil.put(player.getUniqueId(), System.currentTimeMillis() + (delay + spacing * (queuedTitles - 1)) * 50L + stayMillis);
        } else {
            titleBusyUntil.put(player.getUniqueId(), System.currentTimeMillis() + stayMillis);
        }
    }

    private void refresh(Player player, String reason) {
        plugin.getPerkManager().revalidate(player);
        plugin.getPerkMenu().refreshIfOpen(player);
        plugin.getPrestigeManager().refreshIfOpen(player);
        plugin.getStatsMenuManager().refreshIfOpen(player);
        plugin.getBuildScoreboardManager().update(player);
        plugin.getTablistManager().markRefresh(player, reason);
    }

    private int queueFeatureUnlocks(Player player, PitLevelChangeEvent event) {
        if (event.isAdministrative() && !plugin.registry().settings().getBoolean("unlocked-features.unlock-title.trigger-administrative", false)) return 0;
        long delay = Math.max(1L, plugin.registry().settings().getLong("unlocked-features.unlock-title.delay-ticks", 65L));
        long spacing = Math.max(1L, plugin.registry().settings().getLong("unlocked-features.unlock-title.spacing-ticks", 55L));
        String title = plugin.registry().settings().getString("unlocked-features.unlock-title.title", "&e&lFEATURE UNLOCKED!");
        int queued = 0;
        for (LeaderboardManager.FeatureDefinition feature : plugin.getLeaderboardManager().crossedFeatures(event.getOldLevel(), event.getNewLevel())) {
            int index = queued++;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                sendTitle(player, title, feature.display());
            }, delay + spacing * index);
        }
        return queued;
    }

    private int queuePerkSlotUnlocks(Player player, int oldLevel, int newLevel, int queueOffset) {
        if (!plugin.registry().settings().getBoolean("level-up.perk-slot-unlock.enabled", true)) return queueOffset;
        long delay = Math.max(1L, plugin.registry().settings().getLong("level-up.perk-slot-unlock.delay-ticks", 65L));
        long spacing = Math.max(1L, plugin.registry().settings().getLong("level-up.perk-slot-unlock.spacing-ticks", 55L));
        int queued = queueOffset;
        for (int slot = 1; slot <= 4; slot++) {
            int requiredLevel = plugin.registry().perks().getInt("loadout-slots." + slot + ".required-level", 1);
            int requiredPrestige = plugin.registry().perks().getInt("loadout-slots." + slot + ".required-prestige", 0);
            if (oldLevel >= requiredLevel || newLevel < requiredLevel || plugin.getStatsManager().getPrestige(player.getUniqueId()) < requiredPrestige) continue;
            int unlockedSlot = slot;
            long scheduledDelay = delay + spacing * queued++;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                String title = plugin.registry().settings().getString("level-up.perk-slot-unlock.title", "&e&lPERK SLOT UNLOCKED!");
                String subtitle = plugin.registry().settings().getString("level-up.perk-slot-unlock.subtitle", "&7You unlocked Perk Slot #%slot%.").replace("%slot%", String.valueOf(unlockedSlot));
                sendTitle(player, title, subtitle);
            }, scheduledDelay);
        }
        return queued;
    }

    private void playSound(Player player) {
        if (!plugin.registry().settings().getBoolean("level-up.sound.enabled", true) || sound == null) return;
        float volume = (float) Math.max(0.0D, plugin.registry().settings().getDouble("level-up.sound.volume", 1.0D));
        float pitch = (float) Math.max(0.0D, plugin.registry().settings().getDouble("level-up.sound.pitch", 1.0D));
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private void playParticles(Player player) {
        if (!plugin.registry().settings().getBoolean("level-up.particles.enabled", true) || particle == null) return;
        int count = Math.max(1, plugin.registry().settings().getInt("level-up.particles.count", 12));
        float offset = (float) Math.max(0.0D, plugin.registry().settings().getDouble("level-up.particles.offset", 0.6D));
        float speed = (float) Math.max(0.0D, plugin.registry().settings().getDouble("level-up.particles.speed", 0.1D));
        player.getWorld().spigot().playEffect(player.getLocation().add(0.0D, 1.0D, 0.0D), particle, 0, 0, offset, offset, offset, speed, count, 32);
    }

    private Sound parseSound(String name) {
        try {
            return Sound.valueOf(normalize(name));
        } catch (Exception exception) {
            warn("sound", "Invalid level-up sound '" + name + "'. Sound feedback is disabled.");
            return null;
        }
    }

    private Effect parseParticle(String name) {
        try {
            return Effect.valueOf(normalize(name));
        } catch (Exception exception) {
            warn("particle", "Invalid level-up particle '" + name + "'. Particle feedback is disabled.");
            return null;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ENGLISH).replace('-', '_').replace(' ', '_');
    }

    private void warn(String key, String message) {
        if (warnings.add(key)) plugin.getLogger().warning(message);
    }

    private String replace(String input, String oldFormatted, String newFormatted) {
        return CC.color(input.replace("%old_level_formatted%", oldFormatted).replace("%new_level_formatted%", newFormatted));
    }

    private void sendTitle(Player player, String title, String subtitle) {
        int fadeIn = Math.max(0, plugin.registry().settings().getInt("level-up.title.fade-in-ticks", 10));
        int stay = Math.max(0, plugin.registry().settings().getInt("level-up.title.stay-ticks", 40));
        int fadeOut = Math.max(0, plugin.registry().settings().getInt("level-up.title.fade-out-ticks", 10));
        try {
            if (titlePackets == null && !titlePacketsFailed) titlePackets = new TitlePackets();
            if (titlePackets != null) {
                titlePackets.send(player, CC.color(title), CC.color(subtitle), fadeIn, stay, fadeOut);
                return;
            }
        } catch (Throwable throwable) {
            titlePacketsFailed = true;
            titlePackets = null;
            warn("title-packets", "Could not use v1_8_R3 title packets. Falling back to Bukkit title support.");
        }
        try {
            player.sendTitle(CC.color(title), CC.color(subtitle));
        } catch (Throwable throwable) {
            warn("title-fallback", "Level-up titles are unavailable on this server implementation.");
        }
    }

    public long queueTitle(Player player, String title, String subtitle, long delayTicks) {
        if (player == null) return 0L;
        long now = System.currentTimeMillis();
        long requested = now + Math.max(1L, delayTicks) * 50L;
        long start = Math.max(requested, titleBusyUntil.getOrDefault(player.getUniqueId(), 0L));
        long stay = Math.max(1L, plugin.registry().settings().getInt("level-up.title.stay-ticks", 40)) * 50L;
        titleBusyUntil.put(player.getUniqueId(), start + stay);
        long actualDelay = Math.max(1L, (start - now + 49L) / 50L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) sendTitle(player, title, subtitle);
        }, actualDelay);
        return actualDelay;
    }

    private static final class TitlePackets {
        private final Method getHandle;
        private final Field connection;
        private final Method sendPacket;
        private final Method parseComponent;
        private final Constructor<?> timedTitle;
        private final Constructor<?> textTitle;
        private final Object times;
        private final Object title;
        private final Object subtitle;

        private TitlePackets() throws Exception {
            String craftPackage = Bukkit.getServer().getClass().getPackage().getName();
            String version = craftPackage.substring(craftPackage.lastIndexOf('.') + 1);
            String nms = "net.minecraft.server." + version + ".";
            Class<?> craftPlayer = Class.forName(craftPackage + ".entity.CraftPlayer");
            Class<?> entityPlayer = Class.forName(nms + "EntityPlayer");
            Class<?> playerConnection = Class.forName(nms + "PlayerConnection");
            Class<?> packet = Class.forName(nms + "Packet");
            Class<?> component = Class.forName(nms + "IChatBaseComponent");
            Class<?> serializer = Class.forName(nms + "IChatBaseComponent$ChatSerializer");
            Class<?> titlePacket = Class.forName(nms + "PacketPlayOutTitle");
            Class<?> action = Class.forName(nms + "PacketPlayOutTitle$EnumTitleAction");
            getHandle = craftPlayer.getMethod("getHandle");
            connection = entityPlayer.getField("playerConnection");
            sendPacket = playerConnection.getMethod("sendPacket", packet);
            parseComponent = serializer.getMethod("a", String.class);
            timedTitle = titlePacket.getConstructor(action, component, int.class, int.class, int.class);
            textTitle = titlePacket.getConstructor(action, component);
            times = enumValue(action, "TIMES");
            title = enumValue(action, "TITLE");
            subtitle = enumValue(action, "SUBTITLE");
        }

        private void send(Player player, String titleText, String subtitleText, int fadeIn, int stay, int fadeOut) throws Exception {
            Object handle = getHandle.invoke(player);
            Object playerConnection = connection.get(handle);
            Object titleComponent = parseComponent.invoke(null, json(titleText));
            Object subtitleComponent = parseComponent.invoke(null, json(subtitleText));
            sendPacket.invoke(playerConnection, timedTitle.newInstance(times, null, fadeIn, stay, fadeOut));
            sendPacket.invoke(playerConnection, textTitle.newInstance(title, titleComponent));
            sendPacket.invoke(playerConnection, textTitle.newInstance(subtitle, subtitleComponent));
        }

        private static String json(String text) {
            String escaped = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
            return "{\"text\":\"" + escaped + "\"}";
        }

        private static Object enumValue(Class<?> type, String name) {
            for (Object value : type.getEnumConstants()) {
                if (value instanceof Enum<?> enumeration && enumeration.name().equals(name)) return value;
            }
            throw new IllegalArgumentException(name);
        }
    }
}
