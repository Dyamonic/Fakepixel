package club.abelix.api.integration.labymod.service;

import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import net.labymod.serverapi.core.model.feature.Emote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Log4j2
public class LabyNpcEmotesService {

    private static HashMap<UUID, Integer> NPC_EMOTES = new HashMap<>();

    // https://dev.labymod.net/pages/server/labymod/features/emotes/
    public void addNPCEmotes(@NonNull FakeEntityPlayer entityPlayer, int emote) {
        if (emote < 0) {
            return;
        }

        NPC_EMOTES.put(entityPlayer.getUUID(), emote);
        log.info("Added emote {} to labymod to NPC {}", emote, entityPlayer.getUUID());
    }

    public List<Emote> getAllEmotes() {
        List<Emote> emotes = new ArrayList<>();

        NPC_EMOTES.forEach((uuid, emote) -> {
            emotes.add(Emote.play(uuid, emote));
        });

        return emotes;
    }

    public boolean hasNpc() {
        return !NPC_EMOTES.isEmpty();
    }

}
