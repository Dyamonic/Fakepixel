package club.abelix.api.nms.nbt.tag;

import com.google.common.base.Objects;
import lombok.NonNull;

import java.io.DataInputStream;
import java.io.DataOutput;

public class EndTag extends Tag {

    public EndTag() {
        super(TagType.END);
    }

    @Override
    public void push(@NonNull DataOutput output) {
    }

    @Override
    public void pull(@NonNull DataInputStream input) {
    }

    @Override
    public EndTag copy() {
        return new EndTag();
    }

    @Override
    public String toString() {
        throw new UnsupportedOperationException();
    }

    public int hashCode() {
        return Objects.hashCode(type);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof EndTag;
    }
}
