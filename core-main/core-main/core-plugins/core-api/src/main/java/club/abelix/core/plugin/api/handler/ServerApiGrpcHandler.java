package club.abelix.core.plugin.api.handler;

import club.abelix.core.api.proto.v1.GetAllServersRequest;
import club.abelix.core.api.proto.v1.GetAvailableModesRequest;
import club.abelix.core.api.proto.v1.GetAvailableModesResponse;
import club.abelix.core.api.proto.v1.GetMapsByModeRequest;
import club.abelix.core.api.proto.v1.GetMapsByModeResponse;
import club.abelix.core.api.proto.v1.GetModeOnlineRequest;
import club.abelix.core.api.proto.v1.GetModeOnlineResponse;
import club.abelix.core.api.proto.v1.GetOnlineByModeRequest;
import club.abelix.core.api.proto.v1.GetOnlineByModeResponse;
import club.abelix.core.api.proto.v1.GetRunningServersByModeRequest;
import club.abelix.core.api.proto.v1.GetRunningServersRequest;
import club.abelix.core.api.proto.v1.GetRunningServersResponse;
import club.abelix.core.api.proto.v1.GetServerInfoRequest;
import club.abelix.core.api.proto.v1.GetServerInfoResponse;
import club.abelix.core.api.proto.v1.SendPlayerToServerRequest;
import club.abelix.core.api.proto.v1.SendPlayerToServerResponse;
import club.abelix.core.api.proto.v1.ServerApiGrpc;
import club.abelix.core.plugin.api.service.ServerApiService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ServerApiGrpcHandler extends ServerApiGrpc.ServerApiImplBase {

    ServerApiService serverApiService;

    @Override
    public void sendPlayerToServer(SendPlayerToServerRequest request, StreamObserver<SendPlayerToServerResponse> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED.asRuntimeException());
    }

    @Override
    public void getServerInfo(GetServerInfoRequest request, StreamObserver<GetServerInfoResponse> responseObserver) {
        responseObserver.onError(Status.UNIMPLEMENTED.asRuntimeException());
    }

    @Override
    public void getAvailableModes(GetAvailableModesRequest request, StreamObserver<GetAvailableModesResponse> responseObserver) {
        try {
            val response = serverApiService.getAvailableModes();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getRunningServer(GetRunningServersRequest request, StreamObserver<GetRunningServersResponse> responseObserver) {
        try {
            val response = serverApiService.getRunningServers();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getRunningServerByMode(GetRunningServersByModeRequest request, StreamObserver<GetRunningServersResponse> responseObserver) {
        try {
            val response = serverApiService.getRunningServersByMode(request.getMode());

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getOnlineByMode(GetOnlineByModeRequest request, StreamObserver<GetOnlineByModeResponse> responseObserver) {
        try {
            val response = serverApiService.getOnlineByMode(request.getMode());

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        }
    }

    @Override
    public void getMapsByMode(GetMapsByModeRequest request, StreamObserver<GetMapsByModeResponse> responseObserver) {
        try {
            val response = serverApiService.getMapsByMode(request.getMode());

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        }
    }
}
