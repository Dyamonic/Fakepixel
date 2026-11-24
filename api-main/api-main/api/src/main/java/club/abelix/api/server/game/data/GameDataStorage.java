package club.abelix.api.server.game.data;

import club.abelix.api.nms.entity.type.FakeEntity;
import club.abelix.api.nms.team.FakeTeam;
import club.abelix.commons.util.collection.CollectionUtil;
import lombok.Getter;
import org.bukkit.entity.Entity;

import java.util.HashSet;
import java.util.Set;

public class GameDataStorage {

    @Getter
    private final Set<FakeEntity> fakeEntities = new HashSet<>();

    @Getter
    private final Set<FakeTeam> fakeTeams = new HashSet<>();

    private final Set<Entity> entities = CollectionUtil.createConcurrentWeakSet();

    public void store(FakeTeam team) {
        fakeTeams.add(team);
    }

    public void store(Entity entity) {
        entities.add(entity);
    }

    public void store(FakeEntity entity) {
        fakeEntities.add(entity);
    }

    public void remove(FakeTeam team) {
        fakeTeams.remove(team);
    }

    public void remove(FakeEntity entity) {
        fakeEntities.remove(entity);
    }

    public void clearFakeEntities() {
        for (FakeEntity entity : fakeEntities) {
            entity.remove();
        }

        fakeEntities.clear();
    }

    public void clearFakeTeams() {
        for (FakeTeam team : fakeTeams) {
            team.remove();
        }

        fakeTeams.clear();
    }

    public void clearEntities() {
        for (Entity entity : entities) {
            entity.remove();
        }

        entities.clear();
    }

}
