package club.abelix.commons.chatfilter;

import club.abelix.chatfilter.proto.ChatFilterServiceGrpc;
import club.abelix.chatfilter.proto.GetChatLogsRequest;
import club.abelix.chatfilter.proto.GetChatLogsResponse;
import club.abelix.chatfilter.proto.TestFilterRequest;
import club.abelix.chatfilter.proto.TestFilterResponse;
import club.abelix.commons.grpc.AbstractGrpcIntegration;
import lombok.extern.slf4j.Slf4j;
import w.util.Env;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ChatFilterGrpcIntegration extends AbstractGrpcIntegration {

    private final ChatFilterServiceGrpc.ChatFilterServiceStub stub;

    public ChatFilterGrpcIntegration() {
        super(
                Env.getString("CHATFILTER_SERVICE_HOST", "localhost"),
                Env.getInt("CHATFILTER_SERVICE_PORT", 50053)
        );
        this.stub = ChatFilterServiceGrpc.newStub(getChannel())
                .withExecutor(getExecutor());
        log.info("Initialized ChatFilter gRPC integration: {}", getChannel().authority());
    }

    public CompletableFuture<TestFilterResponse> testFilter(String playerName, String message) {
        TestFilterRequest request = TestFilterRequest.newBuilder()
                .setPlayerName(playerName)
                .setMessage(message)
                .build();
        return sendUnary(request, stub::testFilter, 10, TimeUnit.SECONDS);
    }

    public CompletableFuture<GetChatLogsResponse> getChatLogs(String playerName, long fromTimestampMs) {
        GetChatLogsRequest request = GetChatLogsRequest.newBuilder()
                .setPlayerName(playerName)
                .setFromTimestampMs(fromTimestampMs)
                .build();
        return sendUnary(request, stub::getChatLogs, 30, TimeUnit.SECONDS);
    }
}
