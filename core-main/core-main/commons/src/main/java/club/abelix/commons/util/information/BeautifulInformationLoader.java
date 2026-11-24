package club.abelix.commons.util.information;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@Getter
@Deprecated
@RequiredArgsConstructor
public class BeautifulInformationLoader<Instance> {

    private final Class<Instance> cls;
    private final Function<Instance, Object> loader;

    public BeautifulInformationLoader(Function<Instance, Object> loader) {
        this(null, loader);
    }

    public boolean doLoad(Class<?> cls) {
        return this.cls == null || this.cls == cls;
    }

    public String load(Instance instance) {
        return loader.apply(instance).toString();
    }

}
