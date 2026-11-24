package ru.yooxa.http.netty.data;

import io.netty.buffer.ByteBuf;

public class FixedDataReader implements DataReader {

    private final int length;

    public FixedDataReader(int length) {
        this.length = length;
    }

    @Override
    public ByteBuf read(ByteBuf buf) {
        if (buf.readableBytes() < this.length) {
            return null;
        }

        ByteBuf slice = buf.slice(buf.readerIndex(), this.length).retain();
        buf.skipBytes(this.length);

        return slice;
    }
}
