package club.abelix.api.server.base.listener;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.server.lobby.mysterybox.content.SuffixContent;
import club.abelix.api.server.lobby.mysterybox.item.CaseManager;
import club.abelix.commons.data.cases.CaseType;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.data.joinmessage.JoinMessage;
import club.abelix.commons.data.joinmessage.JoinMessageSqlHandler;
import club.abelix.commons.data.suffix.Suffix;
import club.abelix.commons.data.suffix.SuffixSqlHandler;
import club.abelix.commons.data.suffix.color.SuffixColor;
import club.abelix.commons.data.vote.PlayerVoteData;
import club.abelix.commons.data.vote.VotePlatform;
import club.abelix.commons.data.vote.VoteSqlHandler;
import club.abelix.commons.language.Language;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.core.bukkit.event.ConnectionEstablishedEvent;
import club.abelix.core.bukkit.event.ServerActiveEvent;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.play.both.BAddServerJoinMessage;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import club.abelix.network.packet.play.server.*;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class CoreListener extends YListener implements PlayServerProcessor {

    public CoreListener(AbstractPlugin plugin) {
        super(plugin);
    }

    @Override
    public void process(SAddServerSuffix packet, ChannelProcessorContext ctx) {
        int suffixId = packet.getSuffixId();
        String text = packet.getText();
        int cost = packet.getCost();

        SuffixColor color = SuffixColor.getById(packet.getSuffixColorId());
        Rarity rarity = Rarity.getById(packet.getRarityId());

        if (color == null || rarity == null) {
            Logging.IMP.warn("Color or rarity for suffix {} (id: {}) invalid: {}, {}",
                    text, suffixId, color, rarity);

            return;
        }

        Suffix suffix = new Suffix(suffixId, text, color, cost, rarity);
        SuffixSqlHandler.INSTANCE.addServerSuffix(suffix);

        if (rarity != Rarity.NONE) {
            CaseManager.IMP.addItem(CaseType.SUFFIX_CASE, new SuffixContent(rarity, suffix));
        }

        Logging.IMP.info("Created and added new suffix {} (id: {}, rarity: {}, color: {})",
                text, suffixId, rarity, color);
    }

    @Override
    public void process(BAddServerJoinMessage packet, ChannelProcessorContext ctx) {
        int id = packet.getJoinMessageId();

        String name = packet.getName();
        String text = packet.getText();
        int cost = packet.getCost();

        boolean freeForElite = packet.isAvailableForElite();
        boolean custom = packet.isCustom();

        JoinMessage message = new JoinMessage(id, name, text, cost, freeForElite, custom);
        JoinMessageSqlHandler.INSTANCE.addServerJoinMessage(message);

        Logging.IMP.info("Created and added new join message {} (id: {}, name: {}, custom: {}, free for elite: {})",
                id, message.getName(Language.defaultLanguage()), custom, freeForElite);
    }

    @Override
    public void process(SPlayerVote packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null) {
            return;
        }

        VotePlatform platform = VoteSqlHandler.INSTANCE
                .getVotePlatform(packet.getPlatformId());

        if (platform == null) {
            return;
        }

        LocalDateTime timestamp = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(packet.getTs()), ZoneId.systemDefault()
        );

        PlayerVoteData votes = gamer.getPlayerVoteData();
        votes.addVote(platform, timestamp);

        Logging.IMP.info("Add player vote {} (platform - {})",
                gamer.getName(), platform.name());
    }

    @Override
    public void process(SServerCommand packet, ChannelProcessorContext ctx) {
        Logging.IMP.info("Dispatch server command remotely (core-side): /{}",
                packet.getCommand());

        HeartBeat.runSync(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), packet.getCommand()));
    }

    @Override
    public void process(SSendTitle packet, ChannelProcessorContext ctx) {
        Gamer gamer = Gamer.getGamer(packet.getReceiver());

        if (gamer == null) {
            return;
        }

        gamer.sendTitle(
                packet.getTitle(), packet.getSubtitle(),
                packet.getFadeIn(), packet.getStay(), packet.getFadeOut()
        );
    }

    @Override
    public void process(final @NonNull STelegramLinked packet, final @NonNull ChannelProcessorContext ctx) {
        val gamer = Gamer.getGamer(packet.getPlayerId());

        if (gamer == null || !gamer.isOnline()) {
            return;
        }

        if (!gamer.isAuthDataLoaded()) {
            return;
        }

        gamer.getAuthData().setTelegramId(packet.getTelegramId());
    }

    @EventHandler
    public void onActive(ServerActiveEvent event) {
        event.getProcessor().registerProcessor(this);
    }

    @EventHandler
    public void onConnect(ConnectionEstablishedEvent event) {
        Connection.setSlots(Bukkit.getMaxPlayers());
    }
}
