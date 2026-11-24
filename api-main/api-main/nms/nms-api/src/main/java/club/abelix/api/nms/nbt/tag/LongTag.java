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
public class LongTag extends Tag {

    private long value;

    public LongTag(long value) {
        super(TagType.LONG);

        this.value = value;
    }

    public LongTag() {
        this(0);
    }

    public int hashCode() {
        return Objects.hashCode(type, value);
    }

    public boolean equals(Object o) {
        if (!(o instanceof LongTag)) {
            return false;
        }

        LongTag tag = (LongTag) o;

        return tag.value == value;
    }

    @Override
    public void push(@NonNull DataOutput output) throws IOException {
        output.writeLong(value);
    }

    @Override
    public void pull(@NonNull DataInputStream input) throws IOException {
        value = input.readLong();
    }

    @Override
    public LongTag copy() {
        return new LongTag(value);
    }

    @Override
    public String toString() {
        return value + "l";
    }
}
