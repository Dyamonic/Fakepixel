package club.abelix.network.online.data;

import lombok.experimental.UtilityClass;
import club.abelix.network.online.field.ServerField;

import java.util.Locale;

@UtilityClass
public final class Keys {

    public String server(String id) {
        return ("status:server:" + id).toLowerCase(Locale.ROOT);
    }

    public String index(ServerField field, String value) {
        return ("status:index:" + field.getFieldName() + ":" + value).toLowerCase(Locale.ROOT);
    }

    public static final String ALL_SERVERS_INDEX = "status:servers:all";
}
