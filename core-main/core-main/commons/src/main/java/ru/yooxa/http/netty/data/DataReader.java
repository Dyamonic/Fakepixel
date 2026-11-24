package ru.yooxa.http.netty.data;

import io.netty.buffer.ByteBuf;

public interface DataReader {

    /**
     * @param buf Буффер для чтения данных
     * @return Буффер с прочитанными данными
     */
    ByteBuf read(ByteBuf buf);
}
