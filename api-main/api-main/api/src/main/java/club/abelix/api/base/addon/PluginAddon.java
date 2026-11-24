package club.abelix.api.base.addon;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * @author - suuft (aka: swiftyclown, Valeriy Shushpanov) ( vk.com/suuft )
 * Created at 16/01/2023 18:40
 */
@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class PluginAddon {
    String requiredPlugin;

    public abstract void boot();
}
