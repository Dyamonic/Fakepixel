package club.abelix.network.online.info;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import club.abelix.network.online.data.ServerKind;
import club.abelix.network.online.field.ServerField;
import club.abelix.network.online.field.StandardServerFields;

import java.util.List;
import java.util.Map;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public abstract class AbstractServerInfo implements ServerInfo {

    String id;
    String serverName;
    String serverMode;
    ServerKind kind;
    List<String> tags;
    int slots;
    int online;
    int vanishedOnline;

    @Override
    public Map<ServerField, String> getStaticData() {
        return Map.of(
                StandardServerFields.ID, id,
                StandardServerFields.NAME, serverName,
                StandardServerFields.MODE, serverMode,
                StandardServerFields.KIND, kind.name(),
                StandardServerFields.TAG, String.join(",", tags),
                StandardServerFields.SLOTS, Integer.toString(slots)
        );
    }

    @Override
    public Map<ServerField, String> getDynamicData() {
        return Map.of(
                StandardServerFields.ONLINE, Integer.toString(online),
                StandardServerFields.VANISHED_ONLINE, Integer.toString(vanishedOnline)
        );
    }
}
