package club.abelix.api.nms.nbt.tag;

import com.google.common.base.Objects;
import lombok.Getter;
import lombok.NonNull;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;

@Getter
public class ByteTag extends Tag {

    private byte value;

    public ByteTag(byte value) {
        super(TagType.BYTE);

        this.value = value;
    }

    public ByteTag() {
        this((byte) 0);
    }

    public void setValue(int i) {
        value = (byte) i;
    }

    public int getUnsignedValue() {
        return value & 0xFF;
    }

    @Override
    public void push(@NonNull DataOutput output) throws IOException {
        output.writeByte(value);
    }

    @Override
    public void pull(@NonNull DataInputStream input) throws IOException {
        value = input.readByte();
    }

    public int hashCode() {
        return Objects.hashCode(type, value);
    }

    public boolean equals(Object o) {
        if (!(o instanceof ByteTag)) {
            return false;
        }

        ByteTag tag = (ByteTag) o;

        return tag.value == value;
    }

    @Override
    public ByteTag copy() {
        return new ByteTag(value);
    }

    @Override
    public String toString() {
        return value + "b";
    }
}
