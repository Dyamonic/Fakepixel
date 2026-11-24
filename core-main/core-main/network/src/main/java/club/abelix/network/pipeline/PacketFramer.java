package club.abelix.network.pipeline;

import club.abelix.network.packet.PacketUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.CorruptedFrameException;

import java.util.List;

public class PacketFramer extends ByteToMessageCodec<ByteBuf> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf from, ByteBuf to) {
        // Получаем размер пакета.
        int packetSize = from.readableBytes();
        // Вычисляем размер заголовка переменной длины переменной int.
        int headerSize = PacketUtils.getVarIntSize(packetSize);

        // Используем готовый метод ensureWritable для выделения достаточного количества памяти в буфере to.
        // Это позволяет избежать необходимости увеличения размера буфера в процессе кодирования.
        to.ensureWritable(packetSize + headerSize);

        // Записываем размер пакета в переменную длины переменной int.
        PacketUtils.writeVarInt(to, packetSize);
        // Копируем данные из буфера from в буфер to, используя метод writeBytes для избежания итерации по байтам.
        to.writeBytes(from, from.readerIndex(), packetSize);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) {
        // Метод markReaderIndex помечает позицию чтения байтов, чтобы можно было вернуться к этой позиции позже.
        buf.markReaderIndex();

        for (int i = 0; i < 3; ++i) {
            if (!buf.isReadable()) {
                // Если данные в буфере buf еще не готовы для чтения, сбрасываем позицию чтения и возвращаем управление.
                buf.resetReaderIndex();
                return;
            }

            // Считываем один байт из буфера.
            byte b = buf.readByte();

            if (b >= 0) {
                // Если байт положительный, значит он содержит размер пакета в переменной длины переменной int.
                // Сбрасываем позицию чтения к предыдущей метке и считываем размер пакета.
                buf.resetReaderIndex();
                int length = PacketUtils.readVarInt(buf);

                if (buf.readableBytes() < length) {
                    // Если данные в буфере недостаточны для чтения всего пакета,
                    // сбрасываем позицию чтения и возвращаем управление.
                    buf.resetReaderIndex();
                    return;
                }

                // Используем метод slice для создания среза буфера buf, содержащего только нужную часть данных.
                // Используем метод retain, чтобы увеличить счетчик ссылок на буфер и избежать его освобождения до тех пор, пока он используется.
                out.add(buf.slice(buf.readerIndex(), length).retain());
                // Пропускаем прочитанные данные в буфере buf.
                buf.skipBytes(length);
                // Завершаем метод, так как пакет был полностью прочитан.
                return;
            }
        }

        // Если размер пакета превышает 21 бит, то
        throw new CorruptedFrameException("length wider than 21-bit");
    }
}