package club.abelix.network.online.query;

import club.abelix.network.online.data.ServerKind;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class StatusQueries {

    private static final StatusQuery TOTAL_STATUS_QUERY = new StatusQueryBuilder()
            .kind(ServerKind.PROXY)
            .totalOnly()
            .build();

    public StatusQuery totalStatusQuery() {
        return TOTAL_STATUS_QUERY;
    }

    public StatusQueryBuilder parametrizedQuery() {
        return new StatusQueryBuilder();
    }

}
