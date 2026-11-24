package club.abelix.games.fakevival.smpworlds;

import org.bukkit.World;

import java.nio.file.Path;

public record SMPWorld(String worldId, SMPWorldMetadata metadata, SMPWorldState state,
                       Path containerPath, Path runtimePath, World bukkitWorld) {
}
