package club.abelix.api.server.game.factory;

import club.abelix.api.nms.entity.FakeEntityApi;
import club.abelix.api.nms.entity.type.FakeEntity;

public interface EntityFactory<T extends FakeEntity> {

    T createEntity(FakeEntityApi api);

}
