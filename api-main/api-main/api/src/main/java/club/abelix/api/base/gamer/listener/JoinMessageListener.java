package club.abelix.api.base.gamer.listener;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerJoinMessageEvent;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.commons.data.joinmessage.JoinMessage;
import club.abelix.commons.data.setting.Setting;
import club.abelix.commons.language.Language;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.core.bukkit.StarshipBukkit;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class JoinMessageListener extends YListener {

    private final AbstractPlugin plugin;

    /**
     * Время последнего сообщения о входе
     */
    private long lastJoinMessage;

    /**
     * Количество сообщений о входе за последние 1.5 сек
     */
    private int joinMessagesCount;

    private final Cache<Gamer, Long> lastJoinMessageTime = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        val player = event.getPlayer();
        val gamer = Gamer.getGamer(player);
        val shownGroup = gamer.getShownGroup();

        if (StarshipBukkit.getInstance().getRanksService().defaultRank().equals(shownGroup)) {
            return;
        }

        val ranks = gamer.getRanks();

        boolean canApply = ranks.hasPermission("lobby.join.message.elite");
        boolean isElite = gamer.getFakeName() == null && canApply;

        // игрок выключил оповещение о входе
        // (!) хелперы и выше все равно его увидят
        boolean disabledNotification = gamer.isSettingSet(Setting.HIDE_JOIN_MESSAGE);

        if (!disabledNotification) {
            long currentTime = System.currentTimeMillis();

            lastJoinMessageTime.cleanUp();

            Long message = lastJoinMessageTime.getIfPresent(gamer);

            // ДО того как делается проверка, чтобы не получилось
            // что 2 чела из пати зайдут с сообщением, и потом все
            // остальные будут создавать флуд по 2 сообщения разом
            lastJoinMessageTime.put(gamer, currentTime);

            // если за 1.5 секунды больше 2 сообщений о входе, то ничего не отправляем
            // если прошло больше 1.5 сек. то просто сбрасываем счетчик
            if (currentTime - lastJoinMessage > 1_500) {
                joinMessagesCount = 0;
            } else if (joinMessagesCount >= 2) {
                return;
            }

            if (message != null) {
                return;
            }

            GamerJoinMessageEvent joinMessageEvent = new GamerJoinMessageEvent(gamer);
            HeartBeat.callEvent(joinMessageEvent);

            if (joinMessageEvent.isCancelled()) {
                return;
            }

            lastJoinMessage = currentTime;
            joinMessagesCount++;

            if (isElite && !CoreUtil.isUncommon()) {
                shootFireWork(player.getLocation());
            }
        }

        JoinMessage joinMessage = gamer.getActiveJoinMessage();

        String key = "lobby_donate_join_message_format" + (isElite ? "_luxury" : "");

        for (Gamer target : Gamer.getGamers()) {
            if (disabledNotification && (!target.getRanks().hasPermission("disabled.join.messages") || target == gamer)) {
                continue;
            }

            Language language = target.getLanguage();

            String messageText = joinMessage.getText(language);
            String broadcastMessage = language.getMessage(key,
                    "%player%", gamer.getDisplayNameAndSuffix(), "%message%", messageText);

            target.sendMessage(broadcastMessage);
        }
    }

    private void shootFireWork(@NonNull Location location) {
        Firework firework = location.getWorld().spawn(location, Firework.class);

        FireworkMeta fireworkEffectMeta = firework.getFireworkMeta();
        fireworkEffectMeta.addEffect(
                FireworkEffect.builder()
                        .withColor(Color.GREEN)
                        .withFade(Color.ORANGE)
                        .withFlicker()
                        .with(FireworkEffect.Type.BALL)
                        .build()
        );

        firework.setFireworkMeta(fireworkEffectMeta);
    }

}