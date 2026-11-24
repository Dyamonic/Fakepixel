package club.abelix.commons.data.rank.service.mapper;

import club.abelix.commons.data.rank.entity.RankEntity;
import club.abelix.commons.data.rank.entity.UserRankEntity;
import club.abelix.commons.data.rank.type.UserRankType;
import club.abelix.ranks.commons.util.TimestampConverter;
import club.abelix.ranks.proto.RankEntityProto;
import club.abelix.ranks.proto.UserRankEntityProto;
import lombok.experimental.UtilityClass;

import java.util.HashSet;
import java.util.stream.Collectors;

@UtilityClass
public class RanksMapper {

    public UserRankEntityProto mapToProto(UserRankEntity rank) {
        return UserRankEntityProto.newBuilder()
                .setId(rank.getId())
                .setUserId(rank.getUserId())
                .setRank(mapToProto(rank.getRank()))
                .setExpiresAt(TimestampConverter.toProto(rank.getExpires()))
                .build();
    }

    public RankEntityProto mapToProto(RankEntity entity) {
        return RankEntityProto.newBuilder()
                .setId(entity.getId())
                .setName(entity.getName())
                .setDisplayNameKey(entity.getDisplayNameKey())
                .setPriority(entity.getPriority())
                .setPrefix(entity.getPrefix())
                .setSuffix(entity.getSuffix())
                .addAllPermissions(entity.getPermissions())
                .addAllInheritedRanks(entity.getInheritedRanks().stream().map(RanksMapper::mapToProto).toList())
                .build();
    }

    public RankEntity mapFromProto(RankEntityProto e) {
        return new RankEntity(
                e.getId(),
                e.getName(),
                e.getDisplayNameKey(),
                e.getPriority(),
                e.getPrefix(),
                e.getSuffix(),
                new HashSet<>(e.getPermissionsList()),
                e.getInheritedRanksList().stream().map(RanksMapper::mapFromProto).collect(Collectors.toSet())
        );
    }

    public UserRankEntity mapFromProto(UserRankEntityProto proto) {
        return new UserRankEntity(
                proto.getId(),
                proto.getUserId(),
                mapFromProto(proto.getRank()),
                TimestampConverter.fromProto(proto.getExpiresAt()),
                UserRankType.fromProto(proto.getRankType())
        );
    }

}
