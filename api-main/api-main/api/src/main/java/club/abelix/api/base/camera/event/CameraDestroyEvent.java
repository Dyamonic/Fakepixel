package club.abelix.api.base.camera.event;

import club.abelix.api.base.camera.CameraDestroyReason;
import club.abelix.api.base.camera.PlayerCamera;
import lombok.Getter;

@Getter
public class CameraDestroyEvent extends CameraEvent {

    private final CameraDestroyReason reason;

    public CameraDestroyEvent(PlayerCamera camera, CameraDestroyReason reason) {
        super(camera);

        this.reason = reason;
    }
}
