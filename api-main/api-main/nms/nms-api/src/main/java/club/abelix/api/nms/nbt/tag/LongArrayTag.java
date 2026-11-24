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
public class LongArrayTag extends Tag {

    private long[] value;

    public LongArrayTag() {
        this(null);
    }

    public LongArrayTag(long[] value) {
        super(TagType.LONG_ARRAY);

        this.value = value;
    }

    @Override
    public void push(@NonNull DataOutput output) throws IOException {
        output.writeInt(value.length);

        for (long l : value) {
            output.writeLong(l);
        }
    }

    @Override
    public void pull(@NonNull DataInputStream input) throws IOException {
        value = new long[input.readInt()];

        for (int i = 0; i < value.length; i++) {
            value[i] = input.readLong();
        }
    }

    @Override
    public LongArrayTag copy() {
        return new LongArrayTag(value);
    }

    public int hashCode() {
        return Objects.hashCode(type, Arrays.hashCode(value));
    }

    public boolean equals(Object o) {
        if (!(o instanceof LongArrayTag)) {
            return false;
        }

        LongArrayTag tag = (LongArrayTag) o;

        return Arrays.equals(tag.value, value);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[L;");

        for (int i = 0; i < value.length; i++) {
            builder.append(value[i]);

            if (i != value.length - 1) {
                builder.append(",");
            }
        }

        return builder.append("]").toString();
    }

}
