package ru.yooxa.json;

public class JBytesReader {

    public static final String EMPTY_STRING = "";

    private static final int OBJECT = 1;
    private static final int ARRAY = 2;
    private static final int STRING = 3;
    private static final int NUMBER = 4;
    private final byte[] buffer;
    private int index = 0;
    private boolean firstObject = true;

    public JBytesReader(byte[] buffer) {
        if (buffer.length == 0) {
            throw new IllegalArgumentException("Buffer is empty");
        }

        this.buffer = buffer;
    }

    private static int getType(byte first) {
        if (first >= '0' && first <= '9') {
            return NUMBER;
        }

        if (first == '{') {
            return OBJECT;
        }

        if (first == '[') {
            return ARRAY;
        }

        return STRING;
    }

    public JElement read() {
        byte firstChar = skipSpaces();

        int type = getType(firstChar);

        if (this.firstObject) {
            if (type != OBJECT && type != ARRAY) {
                throw new IllegalStateException("Invalid json - " + new String(buffer));
            }

            this.firstObject = false;
        }

        JElement element;
        switch (type) {
            case OBJECT:
                element = readObject();
                break;
            case ARRAY:
                element = readArray();
                break;
            case NUMBER:
            case STRING:
                index--;
                element = readString();
                break;
            default:
                return null;
        }

        return element;
    }

    public JObject readObject() {
        JObject object = new JObject();

        do {
            byte first = skipSpaces();

            if (first == '}') break;

            int[] key;
            if (first == '\"') {
                key = readBefore('\"');

                byte next = skipSpaces();
                if (next != ':') {
                    return null;
                }
            } else {
                index--;
                key = readBefore(':');
            }

            String skey;

            if (key == null) {
                skey = EMPTY_STRING;
            } else {
                skey = new String(this.buffer, key[0], key[1]);
            }

            JElement value = read();

            object.add(skey, value);

            byte next = skipSpaces();
            if (next == '}') break;
        } while (true);


        return object;
    }

    public JArray<JElement> readArray() {
        byte next = skipSpaces();
        if (next == ']') return JArray.EMPTY;
        this.index--;

        JArray<JElement> array = new JArray<>();
        while (true) {
            JElement element = read();
            array.add(element);

            byte c = skipSpaces();
            if (c == ']') {
                break;
            }
        }

        return array;
    }

    public JField readString() {
        byte first = skipSpaces();

        int[] data;
        if (first == '\"') {
            data = readBefore('\"');
        } else {
            index--;
            data = readBefore();
            index--;
        }

        JField field = null;

        if (data != null && !isNull(data[0], data[1])) {
            field = new JField(this.buffer, data[0], data[1]);
        }

        return field;
    }

    private boolean isNull(int offset, int len) {
        if (len != 4) return false;

        return buffer[offset] == 'n' && buffer[offset + 1] == 'u' && buffer[offset + 2] == 'l' && buffer[offset + 3] == 'l';
    }

    private int[] readBefore(char end) {
        int startIndex = this.index;
        boolean start = false;

        byte c;

        do {
            c = this.buffer[index++];

            boolean ekran = false;
            if (c == '\\') {
                ekran = true;
                c = this.buffer[index++];
            }

            if (!start) {
                if (c == ' ') {
                    startIndex = this.buffer[index++];
                } else {
                    start = true;
                }
            }

            if (ekran) continue;

            if (c == end) break;
        } while (true);

        int endIndex = this.index - 1; //Последний символ не является строкой

        int len = endIndex - startIndex;

        if (len <= 0) {
            return null;
        }

        return new int[]{startIndex, len};
    }

    private int[] readBefore() {
        int startIndex = this.index;

        byte c;

        do {
            c = this.buffer[index++];

            boolean ekran = false;
            if (c == '\\') {
                ekran = true;
                c = this.buffer[index++];
            }

            if (!ekran && (c == '\n' || c == ',' || c == '}' || c == ' ' || c == ']')) {
                break;
            }
        } while (true);

        int endIndex = this.index - 1; //Последний символ не является строкой

        int len = endIndex - startIndex;

        if (len <= 0) {
            return null;
        }

        return new int[]{startIndex, len};
    }

    private byte skipSpaces() {
        byte firstChar;
        do {
            firstChar = this.buffer[index++];
        } while (firstChar == ' ' || firstChar == '\n' || firstChar == '\t');

        return firstChar;
    }
}
