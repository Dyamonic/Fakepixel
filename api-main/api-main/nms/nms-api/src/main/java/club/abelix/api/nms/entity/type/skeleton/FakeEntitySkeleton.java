package club.abelix.api.nms.entity.type.skeleton;

import club.abelix.api.nms.entity.type.FakeLivingEntity;
import org.bukkit.entity.Skeleton;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-api</p>
 * <p>FakeEntitySkeleton creation on 11.05.2024 at 14:21</p>
 */

public interface FakeEntitySkeleton extends FakeLivingEntity {
    void setType(Skeleton.SkeletonType type);
}