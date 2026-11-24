package club.abelix.api.nms.nbt.tag;

import com.google.common.base.Objects;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;

@Getter
@Setter
public class IntTag extends Tag {

    private int value;

    public IntTag(int value) {
        super(TagType.INT);

        this.value = value;
    }

    public IntTag() {
        this(0);
    }

    @Override
    public void push(@NonNull DataOutput output) {
        try {
            output.writeInt(value);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void pull(@NonNull DataInputStream input) {
        try {
            value = input.readInt();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int hashCode() {
        return Objects.hashCode(type, value);
    }

    public boolean equals(Object o) {
        if (!(o instanceof IntTag)) {
            return false;
        }

        IntTag tag = (IntTag) o;

        return tag.value == value;
    }

    @Override
    public IntTag copy() {
        return new IntTag(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

}
