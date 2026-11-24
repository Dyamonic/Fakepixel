package club.abelix.network.packet.play.processor;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.play.both.BCheckChannelSubscription;
import club.abelix.network.packet.play.client.*;
import club.abelix.network.packet.play.server.SMovePlayerToMini;
import org.jetbrains.annotations.NotNull;

/**
 * @author WhileInside
 */
public interface PlayClientProcessor extends PlayProcessor {

    default void process(CCreateParty packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CDuelRequest packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CAddPartyMember packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CRemovePartyMember packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CDisbandParty packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CListPartyMembers packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CDispatchCommand packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CActivateNetworkBooster packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CNetworkBoosterRequest packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CQueueQuitQueue packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CBroadcastStaff packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CTabcompleteRequest packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CNetworkPlayerLoadRequest packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CQueueCreate packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CPlayerConnected packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CGetGlobalOnline packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CGetServerOnline packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CGetBestServer packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CReportCheckExistenceRequest packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CReportCheckStateRequest packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CArenaCreate packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CArenaSetState packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CConnectExact packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CConnectToPlayer packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CConnectMode packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CSendServerCommand packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CLookupGlobalOnline packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CSetJoinableState packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CPlayerNickGenerate packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CChangePlayerNick packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CArenaJoinBroadcast packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CSetSlots packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CReconnectPlayers packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CConnectMap packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CProxyPlayerLoginRequest packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CProxyPlayerDisconnect packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CPlayerCommand packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CGetActiveStreams packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CServerConnectRequest packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CReportAction packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CGetReports packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CLoadFriends packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CTwoFactorAuthentication packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CMutePlayer packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CBanPlayer packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SMovePlayerToMini packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CArenaEditQueueState packet, ChannelProcessorContext ctx) {
        // override me
    }


    default void process(CGamerQueueJoinRequest packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CGamerLeaveQueue packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CSendPlayerFailed packet, ChannelProcessorContext ctx) {
        // override me CSendPlayerFailed
    }

    default void process(CGetGuildInfo packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CGiveSkyblockGemsResponse packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CPlayerChangeSkin packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CSyncResponse packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CMapBalancerRegister packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CMapBalancerUpdateMap packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CMapBalancerRecovery packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CPingResponse packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CArenaSetGameMap packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CMakePrivateGameResponse packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CGoPrivateGame packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CPlayerPingResponse packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CServerRestartScheduled packet, @NotNull ChannelProcessorContext ctx) {
        // override me
    }

    default void process(COnlineFriendsCountRequest cOnlineFriendsCountRequest, @NotNull ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CTelegramAuth packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CTelegramMessage packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CServerPingResponse cServerPingResponse, @NotNull ChannelProcessorContext ctx) {

    }

    default void process(BCheckChannelSubscription.Request packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(CTelegramToken packet, ChannelProcessorContext ctx) {
        // override me
    }
}
