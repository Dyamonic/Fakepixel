package club.abelix.api.server.game.box;

import club.abelix.api.base.gamer.Gamer;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Map;

@Log4j2
public class GameWeightStorage {

    private final Map<Gamer, Integer> weights = new HashMap<>();

    public void addWeight(Gamer gamer, int weight) {
        weights.put(gamer, weights.getOrDefault(gamer, 0) + weight);
        log.info("Added weight {} to gamer {}", weight, gamer.getName());
    }

    public int getWeight(Gamer gamer) {
        return weights.getOrDefault(gamer, 0);
    }

    public void reset() {
        weights.clear();
    }
}
