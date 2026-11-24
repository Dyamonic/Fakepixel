package club.abelix.games.buildffa.game.listener.game;

import club.abelix.api.base.event.chat.AsyncChatFormatEvent;
import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.data.BuildFFAGamer;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ChatListener extends YListener {
    private static final String[] BLOCKED_WORDS = {"fuck", "shit", "bitch", "asshole", "nga", "motherfuck", "nigga", "nigg", "nibba", "nigger", "dick", "dik", "stfu", "mfs", "mofo", "milf", "dilf", "gfys", "gtfo", "soab", "pos", "thot", "dtf", "std", "ffv", "Bkl", "bdsm", "Bsdk", "Jhatu", "TMKC", "s&m", "sybau", "Behenchod", "Bhadwa", "Bhosda", "Bhosdike", "Bhosdiwala", "Bhosadchod", "Bur", "Chuchi", "Gaand", "Gandu", "Twat", "Wanker", "Haramjada", "Lauda", "lulli", "lund", "Lodu", "Madarchod", "fucker", "tit", "boob", "retard", "pedo", "shit", "condom", "sex toys", "porn", "hentai", "порно", "порнуха", "хуй", "Penis", "Masturbate", "ебаный", "Трахать", "блять", "Vagina", "вагина", "Whore", "hoe", "Prostitute", "slut", "Honkey", "Beaner", "gooner", "Chink", "Dalit", "ниггер", "нигер", "Pedophile", "Paedophile", "gay", "Jihadi", "Malaun", "Bible beater", "Chuhra", "Saai", "Autistic", "Autism", "Bleach", "Midget", "Castrate", "Twink", "Circumcise", "Fag", "faggot", "пидор", "пидорас", "Cuck", "Dyke", "Incest", "Rape", "Rapist"};
    private static final Pattern BLOCKED_WORD_PATTERN = Pattern.compile(
            "(?<![\\p{L}\\p{M}\\p{N}_])(?:"
                    + Arrays.stream(BLOCKED_WORDS).map(Pattern::quote).collect(Collectors.joining("|"))
                    + ")(?![\\p{L}\\p{M}\\p{N}_])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final String BYPASS_PERMISSION = "buildffa.chatfilter.bypass";
    private static final Pattern EXTRA_SPACES_PATTERN = Pattern.compile("[\\s\\p{Z}]+");
    private static final Pattern MEANINGFUL_CONTENT_PATTERN = Pattern.compile("[\\p{L}\\p{N}\\p{S}]");

    private final BuildFFAGame game;

    public ChatListener(final @NotNull BuildFFAGame game) {
        super(game.getPlugin());
        this.game = game;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPublicChat(final @NotNull AsyncPlayerChatEvent event) {
        if (BLOCKED_WORDS.length == 0) return;
        Matcher matcher = BLOCKED_WORD_PATTERN.matcher(event.getMessage());
        if (!matcher.find()) return;

        Player player = event.getPlayer();
        try {
            boolean bypass = Bukkit.isPrimaryThread()
                    ? game.hasPermission(player, BYPASS_PERMISSION)
                    : Bukkit.getScheduler().callSyncMethod(game.getPlugin(),
                    () -> game.hasPermission(player, BYPASS_PERMISSION)).get();
            if (bypass) return;

            String filtered = EXTRA_SPACES_PATTERN.matcher(matcher.replaceAll("")).replaceAll(" ").trim();
            if (!MEANINGFUL_CONTENT_PATTERN.matcher(ChatColor.stripColor(filtered)).find()) {
                event.setCancelled(true);
                return;
            }
            event.setMessage(filtered);
        } catch (InterruptedException exception) {
            event.setCancelled(true);
            Thread.currentThread().interrupt();
        } catch (ExecutionException exception) {
            event.setCancelled(true);
            throw new IllegalStateException("Could not check chat filter bypass permission", exception.getCause());
        }
    }

    @EventHandler
    public void onChat(final @NotNull AsyncChatFormatEvent event) {
        val sender = BuildFFAGamer.get(event.getSender());
        val receiver = event.getReceiver();

        event.appendFormat(sender.getPrestigeIcon() + sender.getRank().getName(receiver.getLanguage()) + " ");
    }
}
