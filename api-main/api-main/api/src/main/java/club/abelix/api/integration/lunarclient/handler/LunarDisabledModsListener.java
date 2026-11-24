package club.abelix.api.integration.lunarclient.handler;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.listener.YListener;
import com.lunarclient.apollo.Apollo;
import com.lunarclient.apollo.event.ApolloListener;
import com.lunarclient.apollo.event.EventBus;
import com.lunarclient.apollo.event.Listen;
import com.lunarclient.apollo.event.player.ApolloRegisterPlayerEvent;
import com.lunarclient.apollo.mods.impl.ModMinimap;
import com.lunarclient.apollo.module.modsetting.ModSettingModule;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class LunarDisabledModsListener extends YListener implements ApolloListener {
    private final ModSettingModule modSettingModule = Apollo.getModuleManager().getModule(ModSettingModule.class);

    public LunarDisabledModsListener(@NonNull AbstractPlugin plugin) {
        super(plugin);
        EventBus.getBus().register(this);
    }

    @Listen
    public void onServerJoin(ApolloRegisterPlayerEvent event) {
        this.modSettingModule.getOptions().set(event.getPlayer(), ModMinimap.ENABLED, false);
        log.info("Gamer {} disabled use minimap", event.getPlayer().getName());
    }
}
