package ru.yooxa.http.netty;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import ru.yooxa.http.header.Header;

public class HttpEncoder extends MessageToByteEncoder<HttpRequestData> {

    private static final byte[] CRLF = new byte[]{'\r', '\n'};
    private static final byte SPACE = ' ';
    private static final byte SLASH = '/';
    private static final byte[] HTTP_1_1 = "HTTP/1.1".getBytes();

    private static void write(String s, ByteBuf out) {
        out.writeBytes(s.getBytes(Charsets.UTF_8));
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, HttpRequestData msg, ByteBuf out) {
		/*
		GET /api/v1/depth HTTP/1.1
		\r\n
		UserAgent: Chrome
		\r\n
		Connection: Keep-Alive
		\r\n
		\r\n
		data
		 */

        msg.startEncoding = System.currentTimeMillis();

        write(msg.getType().toString(), out);
        out.writeByte(SPACE);
        out.writeByte(SLASH);
        write(msg.getPath(), out);
        out.writeByte(SPACE);
        out.writeBytes(HTTP_1_1);
        out.writeBytes(CRLF);

        for (Header header : msg.getHeaders()) {
            write(header.getName() + ": " + header.getValue(), out);
            out.writeBytes(CRLF);
        }

        out.writeBytes(CRLF);

        if (msg.getData() != null && msg.getData().length != 0) {
            out.writeBytes(msg.getData());
        }

        msg.endEncoding = System.currentTimeMillis();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
    }
}
