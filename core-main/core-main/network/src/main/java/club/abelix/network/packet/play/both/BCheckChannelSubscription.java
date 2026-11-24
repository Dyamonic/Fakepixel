package club.abelix.network.packet.play.both;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import io.netty.buffer.ByteBuf;
import lombok.*;
import org.jetbrains.annotations.NotNull;

public final class BCheckChannelSubscription {

    @Data
    @ToString(callSuper = true)
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class Request extends Packet.Request<PlayClientProcessor, BCheckChannelSubscription.Response> {

        private int playerId;
        private long chatId;

        @Override
        public void process(@NotNull PlayClientProcessor processor, @NotNull ChannelProcessorContext ctx)
                throws Exception {
            processor.process(this, ctx);
        }

        @Override
        public boolean isProcessor(PacketProcessor processor) {
            return processor instanceof PlayClientProcessor;
        }

        @Override
        public void read0(@NonNull ByteBuf buf) throws Exception {
            this.playerId = buf.readInt();
            this.chatId = buf.readLong();
        }

        @Override
        public void write0(@NonNull ByteBuf buf) throws Exception {
            buf.writeInt(playerId);
            buf.writeLong(chatId);
        }

        @Override
        public Class<BCheckChannelSubscription.Response> getResponse() {
            return BCheckChannelSubscription.Response.class;
        }
    }

    @Data
    @ToString(callSuper = true)
    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class Response extends Packet.Response<PlayServerProcessor> {
        private SubscriptionStatus status;

        @Override
        public void process(@NotNull PlayServerProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
            processor.process(this, ctx);
        }

        @Override
        public boolean isProcessor(PacketProcessor processor) {
            return processor instanceof PlayServerProcessor;
        }

        @Override
        public void read0(@NonNull ByteBuf buf) throws Exception {
            this.status = PacketUtils.readEnum(buf, SubscriptionStatus.class);
        }

        @Override
        public void write0(@NonNull ByteBuf buf) throws Exception {
            PacketUtils.writeEnum(buf, status);
        }
    }

    public enum SubscriptionStatus {
        SUBSCRIBED,
        NOT_SUBSCRIBED,
        NOT_LINKED,
        WAS_SUBSCRIBED
    }
}
