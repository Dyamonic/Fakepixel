package club.abelix.api.nms.nbt.tag;

import com.google.common.base.Objects;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

@Getter
@Setter
public class IntArrayTag extends Tag {

    private int[] value;

    public IntArrayTag() {
        this(null);
    }

    public IntArrayTag(int[] value) {
        super(TagType.INT_ARRAY);

        this.value = value;
    }

    @Override
    public void push(@NonNull DataOutput output) throws IOException {
        output.writeInt(value.length);

        for (int i : value) {
            output.writeInt(i);
        }
    }

    @Override
    public void pull(@NonNull DataInputStream input) throws IOException {
        value = new int[input.readInt()];

        for (int i = 0; i < value.length; i++) {
            value[i] = input.readInt();
        }
    }

    public int hashCode() {
        return Objects.hashCode(type, Arrays.hashCode(value));
    }

    public boolean equals(Object o) {
        if (!(o instanceof IntArrayTag)) {
            return false;
        }

        IntArrayTag tag = (IntArrayTag) o;

        return Arrays.equals(tag.value, value);
    }

    @Override
    public IntArrayTag copy() {
        return new IntArrayTag(value);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[I;");

        for (int i = 0; i < value.length; i++) {
            builder.append(value[i]);

            if (i != value.length - 1) {
                builder.append(",");
            }
        }

        return builder.append("]").toString();
    }

}
