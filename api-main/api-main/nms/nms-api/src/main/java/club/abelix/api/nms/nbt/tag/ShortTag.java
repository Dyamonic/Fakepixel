package club.abelix.api.nms.nbt.tag;

import com.google.common.base.Objects;
import lombok.Getter;
import lombok.NonNull;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;

@Getter
public class ShortTag extends Tag {

    private short value;

    public ShortTag(short value) {
        super(TagType.SHORT);

        this.value = value;
    }

    public ShortTag() {
        this((short) 0);
    }

    public void setValue(int i) {
        value = (short) i;
    }

    public int getUnsignedValue() {
        return value & 0xFFFF;
    }

    @Override
    public void push(@NonNull DataOutput output) throws IOException {
        output.writeShort(value);
    }

    @Override
    public void pull(@NonNull DataInputStream input) throws IOException {
        value = input.readShort();
    }

    public int hashCode() {
        return Objects.hashCode(type, value);
    }

    public boolean equals(Object o) {
        if (!(o instanceof ShortTag)) {
            return false;
        }

        ShortTag tag = (ShortTag) o;

        return tag.value == value;
    }

    @Override
    public ShortTag copy() {
        return new ShortTag(value);
    }

    @Override
    public String toString() {
        return value + "s";
    }

}
