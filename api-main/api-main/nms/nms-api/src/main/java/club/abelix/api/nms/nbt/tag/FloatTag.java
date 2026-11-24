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
public class FloatTag extends Tag {

    private float value;

    public FloatTag(float value) {
        super(TagType.FLOAT);

        this.value = value;
    }

    public FloatTag() {
        this(0);
    }

    @Override
    public void push(@NonNull DataOutput output) throws IOException {
        output.writeFloat(value);
    }

    @Override
    public void pull(@NonNull DataInputStream input) throws IOException {
        value = input.readFloat();
    }

    @Override
    public FloatTag copy() {
        return new FloatTag(value);
    }

    @Override
    public String toString() {
        return value + "f";
    }

    public int hashCode() {
        return Objects.hashCode(type, value);
    }

    public boolean equals(Object o) {
        if (!(o instanceof FloatTag)) {
            return false;
        }

        FloatTag tag = (FloatTag) o;

        return tag.value == value;
    }
}
