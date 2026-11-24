package club.abelix.api.base.addon;

import lombok.NonNull;

/**
 * @author - suuft (aka: swiftyclown, Valeriy Shushpanov) ( vk.com/suuft )
 * Created at 16/01/2023 18:40
 */
public interface PluginAddonService {

    void tryLoad(@NonNull Class<? extends PluginAddon> addon);
}
