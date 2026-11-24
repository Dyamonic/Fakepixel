package club.abelix.api.nms.nbt.tag;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NamedCompoundTag extends CompoundTag {

    public static final NamedCompoundTag EMPTY_TAG = new NamedCompoundTag("Empty");

    private final String name;

    @Override
    public CompoundTag copy() {
        NamedCompoundTag copy = new NamedCompoundTag(name);

        forEach((name, tag) -> copy.setTag(name, tag.copy()));

        return copy;
    }
}
