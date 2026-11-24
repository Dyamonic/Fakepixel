package ru.yooxa.utils;

import java.nio.charset.StandardCharsets;

public class YUtils {

    public static final byte[] EMPTY_BUFFER = new byte[0];
    private static final int[] HEX_TENS = new int[8];

    static {
        int hex = 1;

        for (int i = 0; i < HEX_TENS.length; i++) {
            HEX_TENS[i] = hex;
            hex *= 16;
        }
    }

    public static void main(String[] args) {
        System.out.println(parseLong("-12345".getBytes(StandardCharsets.UTF_8), 0, 5));
    }

    public static long parseLong(byte[] buffer, int offset, int len) {
        if (len == 0) {
            return 0;
        }

        boolean negate = false;

        switch (buffer[offset]) {
            case '-':
                negate = true;
            case '+':
                offset++;
        }

        long value = 0;

        for (int i = 0; i < len; i++) {
            value = value * 10 + (buffer[i + offset] & 0xF);
        }

        return negate ? -value : value;
    }

    public static byte[][] toBytes(long l) {
        if (l == 0) {
            return new byte[][]{EMPTY_BUFFER, new byte[]{0, 0}};
        }

        boolean neg = l < 0;
        if (neg) {
            l = -l;
        }

        byte[] buf = new byte[20]; //19 + знак
        byte i = 19;

        while (l > 0) {
            byte c = (byte) (l % 10);
            l /= 10;
            buf[i--] = (byte) (c + '0');
        }

        if (neg) {
            buf[i] = '-';
        } else {
            i++;
        }

        return new byte[][]{buf, new byte[]{i, (byte) (20 - i)}};
    }

    public static int parseInt(byte[] buffer, int offset, int len) {
        if (len == 0) return 0;

        boolean negate = false;

        switch (buffer[offset]) {
            case '-':
                negate = true;
            case '+':
                offset++;
        }

        int value = 0;

        for (int i = 0; i < len; i++) {
            value = value * 10 + (buffer[i + offset] & 0xF);
        }

        return negate ? -value : value;
    }

    public static int parseInt(char[] buffer, int offset, int len) {
        boolean negate = false;

        switch (buffer[offset]) {
            case '-':
                negate = true;
            case '+':
                offset++;
        }

        int value = 0;

        for (int i = 0; i < len; i++) {
            value = value * 10 + (buffer[i + offset] & 0xF);
        }

        return negate ? -value : value;
    }

    public static int parseHex(byte[] buf, int len) {
        int value = 0;

        int i = 0;
        while (len > 0) {
            byte b = buf[i++];

            int a = parseHex(b);
            if (a != 0) {
                a *= HEX_TENS[len - 1];
                value += a;
            }

            len--;
        }

        return value;
    }

    public static int parseHex(byte b) {
        if (b >= '0' && b <= '9') {
            return b - '0';
        }

        if (b >= 'a' && b <= 'f') {
            return 10 + (b - 'a');
        }

        if (b >= 'A' && b <= 'F') {
            return 10 + (b - 'A');
        }

        throw new IllegalStateException("Invalid char " + (char) b);
    }

    public static String format(String format, Object... objects) {
        StringBuilder builder = new StringBuilder();

        char[] chars = format.toCharArray();

        int k = 0;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if (c == '%') {
                //Читаем цифры
                //Читаем точку
                //Читаем цифры
                //читаем букву
                //Хотя кому это нахуй надо, в пизду

                i++;
                builder.append(objects[k++]);
            } else {
                builder.append(c);
            }
        }

        return builder.toString();
    }
}
