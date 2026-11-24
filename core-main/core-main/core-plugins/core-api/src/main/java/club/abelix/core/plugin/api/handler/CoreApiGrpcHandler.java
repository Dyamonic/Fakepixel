package club.abelix.core.plugin.api.handler;

import club.abelix.core.api.proto.v1.CoreApiGrpc;
import club.abelix.core.api.proto.v1.CoreStatusRequest;
import club.abelix.core.api.proto.v1.CoreStatusResponse;
import club.abelix.core.api.proto.v1.GenerateTopRequest;
import club.abelix.core.api.proto.v1.GenerateTopResponse;
import club.abelix.core.api.proto.v1.GetOnlineRequest;
import club.abelix.core.api.proto.v1.GetOnlineResponse;
import club.abelix.core.api.proto.v1.GetRunningStreamsRequest;
import club.abelix.core.api.proto.v1.GetRunningStreamsResponse;
import club.abelix.core.api.proto.v1.StreamRecordProto;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.stream.StreamManager;
import club.abelix.core.plugin.api.service.CoreApiService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.ArrayList;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class CoreApiGrpcHandler extends CoreApiGrpc.CoreApiImplBase {

    CoreApiService coreApiService;

    @Override
    public void getCoreStatus(CoreStatusRequest request, StreamObserver<CoreStatusResponse> responseObserver) {
        respond(responseObserver, coreApiService.getCoreStatus());
    }

    @Override
    public void getOnline(GetOnlineRequest request, StreamObserver<GetOnlineResponse> responseObserver) {
        respond(responseObserver, coreApiService.getGlobalOnline());
    }

    @Override
    public void getRunningStreams(GetRunningStreamsRequest request, StreamObserver<GetRunningStreamsResponse> responseObserver) {
        val streams = StreamManager.INSTANCE.getActiveStreams();

        if (streams.isEmpty()) {
            responseObserver.onNext(GetRunningStreamsResponse.newBuilder().build());
        } else {
            val mappedStreamProto = new ArrayList<StreamRecordProto>();

            streams.forEachEntry((id, stream) -> {
                val streamer = Gamer.getGamer(id);

                if (stream == null) return true;

                val record = StreamRecordProto.newBuilder()
                        .setStreamer(streamer.getName())
                        .setUrl(stream.getStreamPlatform().makeBeautifulUrl(stream))
                        .setViewers(stream.getViewers())
                        .setDuration(System.currentTimeMillis() - stream.getStartedAtServiceTime())
                        .build();

                mappedStreamProto.add(record);
                return true;
            });

            responseObserver.onNext(GetRunningStreamsResponse.newBuilder().addAllStreams(mappedStreamProto).build());
        }

        responseObserver.onCompleted();
    }

    @Override
    public void generateTop(GenerateTopRequest request, StreamObserver<GenerateTopResponse> responseObserver) {
        try {
            coreApiService.generateTop(request).whenComplete((res, throwable) -> {
                if (throwable != null) {
                    respondError(
                            responseObserver,
                            Status.INTERNAL,
                            "Internal error",
                            throwable
                    );
                    return;
                }

                respond(responseObserver, res);
            });
        } catch (StatusRuntimeException statusRuntimeException) {
            respondError(
                    responseObserver,
                    statusRuntimeException.getStatus(),
                    statusRuntimeException.getMessage(),
                    statusRuntimeException
            );
        }
    }

    private <T> void respond(StreamObserver<T> observer, T payload) {
        observer.onNext(payload);
        observer.onCompleted();
    }

    private void respondError(StreamObserver<?> observer, io.grpc.Status status, String message, Throwable cause) {
        val statusBuilder = status.withDescription(message);
        observer.onError(cause != null
                ? statusBuilder.withCause(cause).asRuntimeException()
                : statusBuilder.asRuntimeException());
    }

}
