package club.abelix.api.base.gamehistory;

import club.abelix.api.base.gamehistory.model.GameInfo;
import club.abelix.api.base.gamehistory.model.GamesPage;
import club.abelix.api.base.gamehistory.model.PartyInfo;
import club.abelix.api.base.gamehistory.model.PlayerInfo;
import club.abelix.gamehistory.grpc.Game;
import club.abelix.gamehistory.grpc.GetGamesByPlayerResponse;
import club.abelix.gamehistory.grpc.Party;
import club.abelix.gamehistory.grpc.Player;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GameHistoryConverter {

    public PlayerInfo toPlayerInfo(Player proto) {
        return PlayerInfo.builder()
                .playerId(proto.getPlayerId())
                .playerName(proto.getPlayerName())
                .party(proto.hasParty() ? toPartyInfo(proto.getParty()) : null)
                .metadata(proto.getMetadataMap())
                .build();
    }

    public PartyInfo toPartyInfo(Party proto) {
        return PartyInfo.builder()
                .partyId(proto.getPartyId())
                .leader(proto.hasLeader() ? toPlayerInfo(proto.getLeader()) : null)
                .members(proto.getMembersList().stream().map(GameHistoryConverter::toPlayerInfo).toList())
                .build();
    }

    public GameInfo toGameInfo(Game proto) {
        return GameInfo.builder()
                .gameId(proto.getGameId())
                .mode(proto.getMode())
                .serverName(proto.getServerName())
                .mapName(proto.getMapName())
                .startTime(proto.getStartTime())
                .endTime(proto.getEndTime())
                .totalPlayersCount(proto.getTotalPlayersCount())
                .privateGame(proto.getIsPrivateGame())
                .players(proto.getPlayersList().stream().map(GameHistoryConverter::toPlayerInfo).toList())
                .winners(proto.getWinnersList().stream().map(GameHistoryConverter::toPlayerInfo).toList())
                .build();
    }

    public GamesPage toGamesPage(GetGamesByPlayerResponse proto) {
        return GamesPage.builder()
                .games(proto.getGamesList().stream().map(GameHistoryConverter::toGameInfo).toList())
                .total(proto.getTotal())
                .build();
    }

    public Player toProto(PlayerInfo info) {
        Player.Builder builder = Player.newBuilder()
                .setPlayerId(info.getPlayerId())
                .setPlayerName(info.getPlayerName())
                .putAllMetadata(info.getMetadata());

        if (info.getParty() != null) {
            builder.setParty(toProto(info.getParty()));
        }

        return builder.build();
    }

    public Party toProto(PartyInfo info) {
        Party.Builder builder = Party.newBuilder()
                .setPartyId(info.getPartyId());

        if (info.getLeader() != null) {
            builder.setLeader(toProto(info.getLeader()));
        }

        info.getMembers().forEach(member -> builder.addMembers(toProto(member)));

        return builder.build();
    }

    public Game toProto(GameInfo info) {
        Game.Builder builder = Game.newBuilder()
                .setMode(info.getMode())
                .setServerName(info.getServerName())
                .setMapName(info.getMapName())
                .setStartTime(info.getStartTime())
                .setEndTime(info.getEndTime())
                .setTotalPlayersCount(info.getTotalPlayersCount())
                .setIsPrivateGame(info.isPrivateGame());

        if (info.getGameId() != null) {
            builder.setGameId(info.getGameId());
        }

        info.getPlayers().forEach(p -> builder.addPlayers(toProto(p)));
        info.getWinners().forEach(w -> builder.addWinners(toProto(w)));

        return builder.build();
    }
}
