package club.abelix.network.packet.serialize;

import io.netty.buffer.ByteBuf;

public interface Serializable<T extends Serializable<T>> {

    T serialize(ByteBuf out);

    T deserialize(ByteBuf in);

}
