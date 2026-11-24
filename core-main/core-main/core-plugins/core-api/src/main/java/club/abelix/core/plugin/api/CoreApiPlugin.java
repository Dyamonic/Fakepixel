package club.abelix.core.plugin.api;

import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.plugin.CorePlugin;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.plugin.api.handler.CoreApiGrpcHandler;
import club.abelix.core.plugin.api.handler.PlayerApiGrpcHandler;
import club.abelix.core.plugin.api.handler.ServerApiGrpcHandler;
import club.abelix.core.plugin.api.handler.StaffApiGrpcHandler;
import club.abelix.core.plugin.api.interceptor.RequestsLoggerServerInterceptor;
import club.abelix.core.plugin.api.service.CoreApiService;
import club.abelix.core.plugin.api.service.PlayerApiService;
import club.abelix.commons.grpc.AbstractGrpcIntegration;
import club.abelix.core.plugin.api.service.ServerApiService;
import club.abelix.core.plugin.api.service.StaffApiService;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import w.util.Env;

import java.io.IOException;
import java.net.InetSocketAddress;

@Slf4j
public class CoreApiPlugin extends CorePlugin {

    private final Server server;

    public CoreApiPlugin() {
        val coreApiService = new CoreApiService(
                ServerManager.IMP,
                CoreApplication.getInstance()
        );

        val playerApiService = new PlayerApiService();
        val serverApiService = new ServerApiService();
        val staffApiService = new StaffApiService();

        int port = Env.getInt("CORE_API_PORT", 50051);

        this.server = NettyServerBuilder.forAddress(new InetSocketAddress("0.0.0.0", port))
                .maxInboundMessageSize(AbstractGrpcIntegration.MAX_INBOUND_MESSAGE_SIZE)  
                .addService(new CoreApiGrpcHandler(coreApiService))
                .addService(new PlayerApiGrpcHandler(playerApiService))
                .addService(new ServerApiGrpcHandler(serverApiService))
                .addService(new StaffApiGrpcHandler(staffApiService))
                .intercept(new RequestsLoggerServerInterceptor())
                .build();

        log.info("Core API gRPC server initialized on port {}", port);
    }

    @Override
    public void onEnable() {
        try {
            server.start();
            log.info("Core API gRPC server started, listening on {}", server.getPort());

            getCore().getExecutor().submit(() -> {
                try {
                    server.awaitTermination();
                } catch (InterruptedException e) {
                    log.error("Core API gRPC server interrupted", e);
                }
            });
        } catch (IOException e) {
            log.error("Failed to start Core API gRPC server", e);
        }
    }

    @Override
    public void onDisable() {
        server.shutdownNow();
    }
}
