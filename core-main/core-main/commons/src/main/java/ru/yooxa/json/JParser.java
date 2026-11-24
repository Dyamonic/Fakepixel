package ru.yooxa.json;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

public class JParser {

    public static JElement parse(InputStream in) throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        byte[] buffer = new byte[16 * 1024];
        int n;
        while ((n = in.read(buffer)) > 0) {
            data.write(buffer, 0, n);
        }

        return parse(data.toByteArray());
    }

    public static JElement parse(byte[] bytes) {
        return new JBytesReader(bytes).read();
    }

    public static JElement parse(String s) {
        return parse(s.getBytes());
    }

    public static void write(JObject object, FileWriter writer) throws IOException {
        writer.append(object.toBeautyString());
        writer.flush();
    }

}
