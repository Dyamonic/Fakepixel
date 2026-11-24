package club.abelix.api.server.lobby.impl.lobby.stats.manager;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.lobby.impl.lobby.stats.line.StatsNpcLine;
import club.abelix.api.server.lobby.impl.lobby.stats.npc.StatsNpc;
import club.abelix.network.common.logging.Logging;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatsNpcManager {

    @Getter
    private final List<StatsNpcLine> fields = new ArrayList<>();

    @Getter
    private final ConcurrentHashMap<Gamer, StatsNpc> gamerNpc = new ConcurrentHashMap<>();
    private final Location npcLocation;

    @Setter
    private double holoYAdd = 0.7;

    public List<String> getLines(@NonNull Gamer gamer) {
        List<String> lines = new ArrayList<>();
        fields.forEach(field -> lines.add(field.getLine(gamer)));
        return lines;
    }

    public StatsNpcManager addLine(@NonNull StatsNpcLine line) {
        fields.add(line);
        holoYAdd += 0.4;
        return this;
    }

    public void createNpc(@NonNull Gamer gamer) {
        if (getGamerNpc().containsKey(gamer)) {
            Logging.IMP.info("NPC for gamer " + gamer.getPlayer().getName() + " already exists.");
            return;
        }

        StatsNpc npc = new StatsNpc(npcLocation, gamer, getLines(gamer), holoYAdd);
        npc.spawn();
        getGamerNpc().put(gamer, npc);
        Logging.IMP.info("NPC for gamer " + gamer.getPlayer().getName() + " created at " + npcLocation.toString());
    }

    public void deleteNpc(Gamer gamer) {
        StatsNpc npc = getGamerNpc().remove(gamer);

        if (npc != null) {
            npc.remove();
            Logging.IMP.info("NPC for gamer " + gamer.getPlayer().getName() + " removed.");
        } else {
            Logging.IMP.error("Trying to remove NPC for gamer " + gamer.getPlayer().getName() + " but it is not in cache.");
        }
    }
}
