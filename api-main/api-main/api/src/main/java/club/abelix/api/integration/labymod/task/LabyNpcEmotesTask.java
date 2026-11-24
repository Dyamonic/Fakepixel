package club.abelix.api.integration.labymod.task;

import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.base.util.task.registration.TaskRegistrationProvider;
import club.abelix.api.integration.labymod.service.LabyIntegrationService;
import club.abelix.api.integration.labymod.service.LabyNpcEmotesService;
import lombok.NonNull;
import lombok.val;
import net.labymod.serverapi.core.LabyModProtocol;
import net.labymod.serverapi.core.packet.clientbound.game.feature.EmotePacket;
import net.labymod.serverapi.server.bukkit.LabyModProtocolService;

public class LabyNpcEmotesTask extends AbstractTask {

    private final LabyNpcEmotesService emotesService;
    private final LabyIntegrationService labyIntegrationService;
    private final LabyModProtocol protocol;


    public LabyNpcEmotesTask(@NonNull TaskRegistrationProvider provider, LabyNpcEmotesService emotesService,
                             LabyIntegrationService integrationService) {
        super(provider);
        this.emotesService = emotesService;
        this.labyIntegrationService = integrationService;
        this.protocol = LabyModProtocolService.get().labyModProtocol();
    }

    @Override
    protected void run() {
        if (!emotesService.hasNpc()) return;
        if (!labyIntegrationService.hasPlayers()) return;

        val emotePacket = new EmotePacket(emotesService.getAllEmotes());
        labyIntegrationService.getAllUUID().forEach(uuid -> protocol.sendPacket(uuid, emotePacket));
    }
}