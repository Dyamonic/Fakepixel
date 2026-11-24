package club.abelix.api.base.hotbar_new;

import club.abelix.api.base.hotbar_new.compose.HotbarIconProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StandardHotbarIconProvidersRegistry implements HotbarIconProvidersRegistry {

    Map<String, List<HotbarIconProvider>> registry = new HashMap<>();

    @Override
    public void register(String contextId, HotbarIconProvider c) {
        registry.computeIfAbsent(contextId, k -> new java.util.ArrayList<>()).add(c);
    }

    @Override
    public List<HotbarIconProvider> forContext(String contextId) {
        return registry.get(contextId);
    }

}
