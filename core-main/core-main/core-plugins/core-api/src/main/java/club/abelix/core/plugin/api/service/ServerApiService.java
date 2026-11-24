package club.abelix.core.plugin.api.service;

import club.abelix.core.api.proto.v1.GetAvailableModesResponse;
import club.abelix.core.api.proto.v1.GetMapsByModeResponse;
import club.abelix.core.api.proto.v1.GetModeOnlineResponse;
import club.abelix.core.api.proto.v1.GetOnlineByModeResponse;
import club.abelix.core.api.proto.v1.GetRunningServersResponse;
import club.abelix.core.api.proto.v1.ServerInfoProto;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.connection.server.ArenaServer;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.network.type.mode.Mini;
import club.abelix.network.type.mode.Special;
import club.abelix.network.type.mode.Uncommon;
import club.abelix.network.type.mode.Unknown;
import club.abelix.network.util.ModeUtil;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.Collection;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServerApiService {

    public GetRunningServersResponse getRunningServers() {
        val response = GetRunningServersResponse.newBuilder();

        val servers = mapServers(ServerManager.IMP.getServers());

        response.addAllServers(servers);

        return response.build();
    }

    public GetAvailableModesResponse getAvailableModes() {
        val response = GetAvailableModesResponse.newBuilder();

        val availableMinis = ServerManager.IMP.findAvailableMinis();
        val minis = availableMinis.stream().map(Enum::name).toList();
        val masters = availableMinis.stream().map(Mini::getMaster).map(Enum::name).toList();

        response.addAllMinis(minis);
        response.addAllMaster(masters);

        return response.build();
    }

    public GetRunningServersResponse getRunningServersByMode(String modeName) {
        val mode = ModeUtil.getServerMode(modeName);

        if (mode instanceof Unknown) {
            throw Status.INVALID_ARGUMENT.withDescription("Unknown mode").asRuntimeException();
        }

        val response = GetRunningServersResponse.newBuilder();

        val servers = mapServers(ServerManager.IMP.getServers(mode));

        response.addAllServers(servers);

        return response.build();
    }

    private List<ServerInfoProto> mapServers(Collection<AbstractServer> servers) {
        return servers.stream()
                .map(server -> ServerInfoProto.newBuilder()
                        .setName(server.getName())
                        .setState(server.getServerState().name())
                        .setType(server.getType().getMode().getName())
                        .setOnline(server.getOnline()).setHost(server.getHost())
                        .setPort(String.valueOf(server.getPort()))
                        .build())
                .toList();
    }

    public GetOnlineByModeResponse getOnlineByMode(String modeName) {
        val mode = ModeUtil.getServerMode(modeName);

        if (mode instanceof Unknown) {
            throw Status.INVALID_ARGUMENT.withDescription("Unknown mode").asRuntimeException();
        }

        val response = GetOnlineByModeResponse.newBuilder();

        response.setOnline(ServerManager.IMP.getOnlineByMode(mode));
        response.setMode(modeName);

        return response.build();
    }

    public GetMapsByModeResponse getMapsByMode(String modeName) {
        val mode = ModeUtil.getServerMode(modeName);

        if (mode instanceof Unknown) {
            throw Status.INVALID_ARGUMENT.withDescription("Unknown mode").asRuntimeException();
        }

        val response = GetMapsByModeResponse.newBuilder();
        val maps = ServerManager.IMP.getServers(mode)
                .stream()
                .filter(server -> server instanceof ArenaServer)
                .map(server -> (ArenaServer) server)
                .map(ArenaServer::getMapName)
                .toList();

        response.addAllMaps(maps);

        return response.build();
    }
}
