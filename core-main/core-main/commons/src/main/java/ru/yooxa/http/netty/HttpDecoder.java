package ru.yooxa.http.netty;

import club.abelix.network.common.logging.Logging;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import ru.yooxa.http.header.Header;
import ru.yooxa.http.netty.data.ChunkDataReader;
import ru.yooxa.http.netty.data.DataReader;
import ru.yooxa.http.netty.data.EmptyDataReader;
import ru.yooxa.http.netty.data.FixedDataReader;
import ru.yooxa.utils.YUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpDecoder extends ByteToMessageDecoder {

    private static final int BUFFER_SIZE = 512;

    private static final int VERSION = 0;
    private static final int RESPONSE_CODE = 1;
    private static final int RESPONSE_MESSAGE = 2;
    private static final int HEADER_KEY = 3;
    private static final int HEADER_VALUE = 4;
    private static final int DATA = 5;
    private final char[] stringBuffer = new char[1024];
    private final byte[] readBuffer = new byte[BUFFER_SIZE];
    private int state = VERSION;
    private int len = 0;
    private long firstByte = -1;
    private String version;
    private int code;
    private String message;
    private String headerKey;
    private DataReader dataReader;
    private Map<String, Header> headers = new HashMap<>();

    public HttpDecoder() {
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (this.state < DATA) {
            if (this.firstByte == -1) {
                this.firstByte = System.currentTimeMillis();
            }

            int readable = in.readableBytes();

            while (readable > 0) {
                int toRead = Math.min(readable, BUFFER_SIZE);
                readable -= toRead;

                int bufIndex = in.readerIndex(); //Запоминаем где начинали читать

                in.readBytes(this.readBuffer, 0, toRead); //Читаем байты

                int index = handleBytes(toRead);
                if (index < 0) continue;

                in.readerIndex(bufIndex + index + 1); //Возвращаем все что непрочитали и пропускаем \n

                int dataLength = 0;
                String transfer = null;

                Header temp = this.headers.get("content-length");
                if (temp != null) {
                    dataLength = Integer.parseInt(temp.getValue());
                }
                temp = this.headers.get("transfer-encoding");
                if (temp != null) {
                    transfer = temp.getValue();
                }

                if (dataLength > 0) {
                    this.dataReader = new FixedDataReader(dataLength);
                } else if (transfer != null && transfer.equalsIgnoreCase("chunked")) {
                    this.dataReader = new ChunkDataReader(ctx.alloc());
                } else {
                    this.dataReader = new EmptyDataReader();
                }
                break;
            }

            if (readable == 0) return;
        }

        if (this.state != DATA) return;

        ByteBuf dataBuffer = this.dataReader.read(in);
        if (dataBuffer == null) {
            return;
        }

        byte[] data = new byte[dataBuffer.readableBytes()];
        dataBuffer.readBytes(data).release();

		/* Это сжатие, но его нахуй не надо использовать
		String encoding = null;
		Header temp = this.headers.get("content-encoding");
		if(temp != null) {
			encoding = temp.getValue();
		}

		if (encoding != null && data.length > 0) {
			if (encoding.equalsIgnoreCase("gzip")) {
				int len = data.length;

				ByteArrayOutputStream decoded = new ByteArrayOutputStream(data.length * 4);

				GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data));

				byte[] buf = new byte[len];
				int n;
				while ((n = gzip.read(buf)) > 0) {
					decoded.write(buf, 0, n);
				}

				gzip.close();

				data = decoded.toByteArray(); //Переписать если будем использовать gzip (Надеюсь не будем)
			} else {
				throw new IllegalStateException("Unknown encoding");
			}
		}
		 */

        HttpResponse response = new HttpResponse(this.code, this.message, this.headers, data);
        response.setReadFirstTime(this.firstByte);
        response.setReadFullTime(System.currentTimeMillis());

        out.add(response);
        reset();
    }

    private void reset() {
        this.state = VERSION;
        this.dataReader = null;
        this.headers = new HashMap<>();
        this.len = 0;
    }

    private void addHeader(String key, String value) {
        this.headers.put(key.toLowerCase(), new Header(key, value));
    }

    private int handleBytes(int size) {
        for (int i = 0; i < size; i++) {
            byte b = this.readBuffer[i];
            if (b == '\r') {
                continue;
            }

            if (this.state < RESPONSE_MESSAGE) {
                if (b == ' ') {
                    if (this.state == VERSION) {
                        this.version = new String(this.stringBuffer, 0, this.len);
                    } else {
                        this.code = YUtils.parseInt(this.stringBuffer, 0, this.len);
                    }

                    this.len = 0;
                    this.state++;
                    continue;
                }
            } else {
                if (this.state == HEADER_VALUE && this.len == 0 && b == ' ') {
                    continue;
                }

                if (this.state == HEADER_KEY && b == ':') {
                    this.headerKey = new String(this.stringBuffer, 0, this.len);
                    this.len = 0;
                    this.state++;
                    continue;
                }

                if (b == '\n') {
                    switch (this.state) {
                        case RESPONSE_MESSAGE:
                            this.message = new String(this.stringBuffer, 0, this.len);
                            this.state++;
                            break;

                        case HEADER_KEY:
                            this.state = DATA;
                            return i; //Текущий символ + \n

                        case HEADER_VALUE:
                            String headerValue = new String(this.stringBuffer, 0, this.len);

                            this.addHeader(headerKey, headerValue);
                            this.state = HEADER_KEY;
                            break;
                    }

                    this.len = 0;
                    continue;
                }
            }

            if (this.len == this.stringBuffer.length) {
                Logging.IMP.info(new String(this.stringBuffer));
            }

            this.stringBuffer[this.len++] = (char) b;
        }

        return -1;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Logging.IMP.stacktrace(cause, "Error occurred whilst decoding response");
    }
}