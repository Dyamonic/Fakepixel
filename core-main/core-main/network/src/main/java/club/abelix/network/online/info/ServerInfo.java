package club.abelix.network.online.info;

import club.abelix.network.online.data.ServerKind;
import club.abelix.network.online.field.ServerField;

import java.util.List;
import java.util.Map;

public interface ServerInfo {

    String getId();

    String getServerName();

    String getServerMode();

    List<String> getTags();

    ServerKind getKind();

    Map<ServerField, String> getStaticData();

    Map<ServerField, String> getDynamicData();

}
