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
public class ByteArrayTag extends Tag {

    private byte[] value;

    public ByteArrayTag() {
        this(null);
    }

    public ByteArrayTag(byte[] value) {
        super(TagType.BYTE_ARRAY);

        this.value = value;
    }

    @Override
    public void push(@NonNull DataOutput output) throws IOException {
        if (value == null) {
            throw new NullPointerException("value");
        }

        output.writeInt(value.length);
        output.write(value);
    }

    @Override
    public void pull(@NonNull DataInputStream input) throws IOException {
        value = new byte[input.readInt()];

        input.readFully(value);
    }

    public int hashCode() {
        return Objects.hashCode(type, Arrays.hashCode(value));
    }

    public boolean equals(Object o) {
        if (!(o instanceof ByteArrayTag)) {
            return false;
        }

        ByteArrayTag tag = (ByteArrayTag) o;

        return Arrays.equals(tag.value, value);
    }

    @Override
    public ByteArrayTag copy() {
        return new ByteArrayTag(value);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[B;");

        for (int i = 0; i < value.length; i++) {
            builder.append(value[i]);

            if (i != value.length - 1) {
                builder.append(",");
            }
        }

        return builder.append("]").toString();
    }

}
