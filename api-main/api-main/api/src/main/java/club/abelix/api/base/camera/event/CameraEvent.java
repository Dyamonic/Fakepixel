package club.abelix.api.base.camera.event;

import club.abelix.api.base.camera.PlayerCamera;
import club.abelix.core.bukkit.event.YEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class CameraEvent extends YEvent {

    private final PlayerCamera camera;

}
