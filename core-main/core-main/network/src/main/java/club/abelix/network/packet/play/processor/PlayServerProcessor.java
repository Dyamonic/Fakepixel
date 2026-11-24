package club.abelix.network.packet.play.processor;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.play.both.BCheckChannelSubscription;
import club.abelix.network.packet.play.server.*;
import org.jetbrains.annotations.NotNull;

/**
 * @author WhileInside
 */
public interface PlayServerProcessor extends PlayProcessor {

    default void process(SIgnoreListUpdate packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SPartyMemberResponse packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SUpdatePrefix packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SNetworkPlayerLoadResponse packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SRemovePrefix packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(STabcompleteResponse packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SMovePlayerToMini packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SPlayerVote packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(STablistHeaderFooter packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SReloadLanguages packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SFailedReconnectPlayers packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SSetGlobalOnline packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SServerCommand packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SServerConnectResponse packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SAddPlayerSuffix packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SAddServerSuffix packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SSendTitle packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SConnectResponse packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SSendPlayer packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SProxyPlayerLoginResponse packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SRegisterCommands packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SRegisterServers packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SUnregisterServer packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SActiveStreams packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SMuteChangeMuteState packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SUnmute packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SPlayerNickGenerateResponse packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SNetworkBoosterResponse packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SChangePlayerNickResponse packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SReportCheckStateResponse packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SReports packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SReportCheckExistenceResponse packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SPartyChange packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SPartyChangeLeader packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SPartyRemove packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SFriendAction packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SGlobalOnline packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SServerOnline packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SBestServer packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SAddPlayerJoinMessage packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(STwoFactorAuthenticationResult packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SGamerQueueJoinResponse packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SGuildInfo packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SUpdatePlayersTag packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SGiveSkyblockGems packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SSyncRequest packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SMapBalancerUpdateResponse packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SDuelOpenGui packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SServerStatusUpdate packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SPingRequest packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SDisableGameStatistic packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SGiveCompanion packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SMakeGamePrivateRequest packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SPlayerPingRequest packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SRefreshRanksPacket packet, @NotNull ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SOnlineFriendsCountResponse packet, @NotNull ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SScreenShareStartPacket packet, @NotNull ChannelProcessorContext ctx) {

    }

    default void process(SScreenShareFinishPacket sScreenShareFinishPacket, @NotNull ChannelProcessorContext ctx) {

    }

    default void process(STelegramAuth packet, @NotNull ChannelProcessorContext ctx) {
        // override me
    }

    default void process(STelegramLinked packet, @NotNull ChannelProcessorContext ctx) {
        // override me
    }

    default void process(SServerPingRequest sServerPingRequest, @NotNull ChannelProcessorContext ctx) {

    }

    default void process(BCheckChannelSubscription.Response packet, ChannelProcessorContext ctx) {
        // override me
    }

    default void process(STelegramToken packet, ChannelProcessorContext ctx) {
        // override me
    }
}
