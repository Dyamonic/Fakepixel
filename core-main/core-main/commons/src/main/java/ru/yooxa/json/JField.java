package ru.yooxa.json;

import lombok.Getter;
import ru.yooxa.utils.YUtils;

import java.math.BigDecimal;

public class JField extends JElement {

    @Getter
    private final byte[] buffer;
    @Getter
    private final int offset;
    @Getter
    private final int len;
    private String cache;

    public JField(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    public JField(byte[] bytes, int offset, int len) {
        this.buffer = bytes;
        this.offset = offset;
        this.len = len;
    }

    public JField(long l) {
        byte[][] temp = YUtils.toBytes(l);

        this.buffer = temp[0];
        this.offset = temp[1][0];
        this.len = temp[1][1];
    }

    public JField(String s) {
        this(s.getBytes());
        this.cache = s;
    }

    public JField(BigDecimal d) {
        this(d.toPlainString());
    }

    @Override
    public String toString() {

        if (this.cache == null) {
            if (this.buffer.length == 0) {
                this.cache = "0";
            } else {
                this.cache = new String(this.buffer, this.offset, this.len);
            }
        }

        return this.cache;
    }

    @Override
    public JElement copy() {
        byte[] data = new byte[len];
        System.arraycopy(this.buffer, offset, data, 0, this.len);

        return new JField(data);
    }

    @Override
    public int getAsInt() {
        return YUtils.parseInt(this.buffer, this.offset, this.len);
    }

    @Override
    public long getAsLong() {
        return YUtils.parseLong(this.buffer, this.offset, this.len);
    }

    public long getAsLong(int subFirst, int subLast) {
        return YUtils.parseLong(this.buffer, this.offset + subFirst, this.len - subLast);
    }

    @Override
    public String getAsString() {
        return this.toString();
    }

    @Override
    public double getAsDouble() {
        return Double.parseDouble(toString());
    }

    @Override
    public BigDecimal getAsBigDecimal() {
        return new BigDecimal(toString());
    }

    public boolean isString() {
        return !isNumber() && !isBoolean();
    }

    public boolean isNumber() {
        for (int i = 0; i < len; i++) {
            byte b = this.buffer[this.offset + i];

            if ((b < '0' || b > '9') && (b < '+' || b > '.') && b != 'e') {
                return false;
            }
        }

        return true;
    }

    public boolean isBoolean() {
        int len = this.len;

        if (len != 4 && len != 5) {
            return false;
        }

        if (len == 4) {
            return buffer[offset] == 't' && buffer[offset + 1] == 'r' && buffer[offset + 2] == 'u' && buffer[offset + 3] == 'e';
        } else {
            return buffer[offset] == 'f' && buffer[offset + 1] == 'a' && buffer[offset + 2] == 'l' && buffer[offset + 3] == 's' && buffer[offset + 4] == 'e';
        }
    }

    @Override
    public boolean getAsBoolean() {
        if (this.len == 4) {
            if (buffer[offset] == 't' && buffer[offset + 1] == 'r' && buffer[offset + 2] == 'u' && buffer[offset + 3] == 'e') {
                return true;
            } else {
                throw new IllegalStateException("Not boolean");
            }
        } else if (this.len == 5) {
            if (buffer[offset] == 'f' && buffer[offset + 1] == 'a' && buffer[offset + 2] == 'l' && buffer[offset + 3] == 's' && buffer[offset + 4] == 'e') {
                return false;
            } else {
                throw new IllegalStateException("Not boolean");
            }
        } else {
            throw new IllegalStateException("Not boolean");
        }
    }
}
