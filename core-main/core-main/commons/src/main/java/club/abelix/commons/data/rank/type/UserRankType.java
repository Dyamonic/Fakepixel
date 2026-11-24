package club.abelix.commons.data.rank.type;

public enum UserRankType {

    PERMANENT, TEMPORARY, UNSPECIFIED;

    public static UserRankType fromProto(club.abelix.ranks.proto.UserRankType type) {
        return switch (type) {
            case USER_RANK_TYPE_UNSPECIFIED, UNRECOGNIZED -> UNSPECIFIED;
            case USER_RANK_TYPE_TEMPORARY -> TEMPORARY;
            case USER_RANK_TYPE_PERMANENT -> PERMANENT;
        };
    }

}
