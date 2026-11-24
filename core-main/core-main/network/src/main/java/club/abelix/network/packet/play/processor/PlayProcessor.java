package club.abelix.network.packet.play.processor;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.play.both.*;
import club.abelix.network.packet.play.client.CSync;

/**
 * @author WhileInside
 */
public interface PlayProcessor extends PacketProcessor {

    default void process(BQueueJoinQueue packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(BProxyKick packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(BUserRankRefreshPacket packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(BUpdatePlayerLanguage packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(BSetProperty packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(BRemoveProperty packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(BChat packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(BBroadcastChat packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(BAddServerJoinMessage packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CSync packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(BBroadcastChatLocalized packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(BPlayerSuccessBotCheck packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(BBestPlayerDonate packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(BBestPlayerUpdate packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(BProxyBotCheck packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(BLicenseStatus packet, ChannelProcessorContext ctx) {
        // override me
    }
}
