package club.abelix.api.server.lobby.impl.hub.present;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public record Present(int id, @NotNull Location location) {}
