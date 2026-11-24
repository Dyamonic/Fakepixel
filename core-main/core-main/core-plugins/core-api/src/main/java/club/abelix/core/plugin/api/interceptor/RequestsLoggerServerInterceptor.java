package club.abelix.core.plugin.api.interceptor;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class RequestsLoggerServerInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        long startNanos = System.nanoTime();
        String op = call.getMethodDescriptor().getBareMethodName();

        ServerCall<ReqT, RespT> loggingCall =
                new ForwardingServerCall.SimpleForwardingServerCall<>(call) {

                    @Override
                    public void close(Status status, Metadata trailers) {
                        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
                        if (status.isOk()) {
                            log.debug("[CoreApi] [{}] completed in {} ms", op, ms);
                        } else {
                            log.error("[CoreApi] [{}] failed in {} ms", op, ms);
                            log.error("[CoreApi] Status: {}", status, status.getCause());
                        }
                        super.close(status, trailers);
                    }
                };

        return next.startCall(loggingCall, headers);
    }
}
