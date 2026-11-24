package club.abelix.api.base.hotbar_new;

import club.abelix.api.base.hotbar_new.compose.HotbarIconProvider;

import java.util.List;

public interface HotbarIconProvidersRegistry {

    void register(String hotbarId, HotbarIconProvider c);

    List<HotbarIconProvider> forContext(String hotbarId);
}
