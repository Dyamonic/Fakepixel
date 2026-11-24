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
public class DoubleTag extends Tag {

    private double value;

    public DoubleTag(double value) {
        super(TagType.DOUBLE);

        this.value = value;
    }

    public DoubleTag() {
        this(0);
    }

    @Override
    public void push(@NonNull DataOutput output) throws IOException {
        output.writeDouble(value);
    }

    @Override
    public void pull(@NonNull DataInputStream input) throws IOException {
        value = input.readDouble();
    }

    public int hashCode() {
        return Objects.hashCode(type, value);
    }

    public boolean equals(Object o) {
        if (!(o instanceof DoubleTag)) {
            return false;
        }

        DoubleTag tag = (DoubleTag) o;

        return tag.value == value;
    }

    @Override
    public DoubleTag copy() {
        return new DoubleTag(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

}
