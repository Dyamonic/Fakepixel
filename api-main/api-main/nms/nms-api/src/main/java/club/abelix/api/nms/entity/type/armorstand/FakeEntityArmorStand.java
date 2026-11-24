package club.abelix.api.nms.entity.type.armorstand;

import club.abelix.api.nms.entity.type.FakeLivingEntity;
import com.comphenix.protocol.wrappers.Vector3F;

public interface FakeEntityArmorStand extends FakeLivingEntity {

    void setLeftLegPose(Vector3F vector);

    void setRightLegPose(Vector3F vector);

    void setRightArmPose(Vector3F vector);

    void setLeftArmPose(Vector3F vector);

    void setBodyPose(Vector3F vector);

    void setHeadPose(Vector3F vector);

    Vector3F getHeadPose();

    Vector3F getBodyPose();

    Vector3F getLeftArmPose();

    Vector3F getRightArmPose();

    Vector3F getLeftLegPose();

    Vector3F getRightLegPose();

    void setBasePlate(boolean plate);

    void setInvisible(boolean invisible);

    void setArms(boolean arms);

    void setSmall(boolean small);

    boolean hasBasePlate();

    boolean hasArms();

    boolean isSmall();

    void setMarker(boolean maker);

    boolean isMarker();

}
