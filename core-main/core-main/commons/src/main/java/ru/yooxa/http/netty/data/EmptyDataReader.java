package ru.yooxa.http.netty.data;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class EmptyDataReader implements DataReader {
    @Override
    public ByteBuf read(ByteBuf buf) {
        return Unpooled.EMPTY_BUFFER;
    }
}
