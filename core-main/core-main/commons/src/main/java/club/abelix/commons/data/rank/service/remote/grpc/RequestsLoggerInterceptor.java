package club.abelix.commons.data.rank.service.remote.grpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class RequestsLoggerInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOpts,
            Channel next) {

        long startNanos = System.nanoTime();
        ClientCall<ReqT, RespT> call = next.newCall(method, callOpts);

        return new ForwardingClientCall.SimpleForwardingClientCall<>(call) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                Listener<RespT> timingListener =
                        new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(responseListener) {
                            @Override
                            public void onClose(Status status, Metadata trailers) {
                                long ms =
                                        TimeUnit.NANOSECONDS.toMillis(
                                                System.nanoTime() - startNanos);
                                String op = method.getBareMethodName();

                                if (status.isOk()) {
                                    log.debug("[{}] completed in {} ms", op, ms);
                                } else {
                                    log.error("[{}] failed in {} ms", op, ms);
                                    log.error("Status: {}", status, status.getCause());
                                }
                                super.onClose(status, trailers);
                            }
                        };

                super.start(timingListener, headers);
            }
        };
    }
}
