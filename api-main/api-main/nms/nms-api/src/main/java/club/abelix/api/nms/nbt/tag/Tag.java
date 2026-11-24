package club.abelix.api.nms.nbt.tag;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;

@AllArgsConstructor
@Getter
public abstract class Tag {

    protected TagType<?> type;

    public abstract void push(@NonNull DataOutput output) throws IOException;

    public abstract void pull(@NonNull DataInputStream input) throws IOException;

    public abstract Tag copy();

    public abstract String toString();

    public int getTypeId() {
        return type.getTypeId();
    }

}
