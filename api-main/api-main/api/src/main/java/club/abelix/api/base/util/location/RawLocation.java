package club.abelix.api.base.util.location;

import lombok.NonNull;

public record RawLocation(@NonNull String world,
                          double x,
                          double y,
                          double z,
                          float yaw,
                          float pitch) {
}
