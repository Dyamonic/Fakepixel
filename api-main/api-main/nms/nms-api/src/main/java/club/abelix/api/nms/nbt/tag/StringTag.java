package club.abelix.api.nms.nbt.tag;

import com.google.common.base.Objects;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.util.regex.Pattern;

@Getter
@Setter
public class StringTag extends Tag {

    public static Pattern valuePattern = Pattern.compile("[a-zA-Z0-9]");

    private String value;

    public StringTag(String value) {
        super(TagType.STRING);

        this.value = value;
    }

    public StringTag() {
        this(null);
    }

    @Override
    public void push(@NonNull DataOutput output) throws IOException {
        output.writeUTF(value);
    }

    @Override
    public void pull(@NonNull DataInputStream input) throws IOException {
        value = input.readUTF();
    }

    @Override
    public StringTag copy() {
        return new StringTag(value);
    }

    public int hashCode() {
        return Objects.hashCode(type, value);
    }

    public boolean equals(Object o) {
        if (!(o instanceof StringTag)) {
            return false;
        }

        StringTag tag = (StringTag) o;

        return Objects.equal(tag.value, value);
    }

    @Override
    public String toString() {
        return valuePattern.matcher(value).matches() ? value : escape(value);
    }

    public static String escape(String s) {
        return "\"" + s.replace("\"", "\\\"") + "\"";
    }

}
