package ru.yooxa.http.netty.data;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import ru.yooxa.utils.YUtils;

public class ChunkDataReader implements DataReader {

    private final CompositeByteBuf data;
    private final byte[] hexBuf = new byte[10];

    private int hexLen = 0;
    private int dataLength = -1;

    public ChunkDataReader(ByteBufAllocator allocator) {
        this.data = allocator.compositeBuffer();
    }

    @Override
    public ByteBuf read(ByteBuf buf) {
        int len = buf.readableBytes();

        if (this.dataLength == -1) {
            this.dataLength = readHex(buf);

            if (this.dataLength == -1) {
                return null;
            }

            len = buf.readableBytes();
        }

        if (len < this.dataLength + 2) return null;

        if (this.dataLength > 0) {
            ByteBuf slice = buf.slice(buf.readerIndex(), this.dataLength).retain();
            data.addComponent(slice);
            data.writerIndex(data.writerIndex() + slice.readableBytes());

            buf.skipBytes(this.dataLength + 2);
            this.dataLength = -1;
            return null;
        } else {
            buf.skipBytes(2); // \r\n
            return this.data;
        }
    }

    private int readHex(ByteBuf in) {
        int readable = in.readableBytes();

        byte b;
        while ((b = in.readByte()) != '\n') {
            readable--;

            if (b == '\r') {
                if (readable == 0) {
                    return -1;
                } else {
                    continue;
                }
            }

            if (this.hexLen == this.hexBuf.length) {
                throw new IllegalStateException("Too big hex value");
            }

            this.hexBuf[this.hexLen++] = b;

            if (readable == 0) {
                return -1;
            }

        }

        int hex = YUtils.parseHex(this.hexBuf, this.hexLen);
        this.hexLen = 0;

        return hex;
    }
}