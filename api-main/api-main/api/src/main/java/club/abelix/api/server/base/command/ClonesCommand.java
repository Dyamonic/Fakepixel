package club.abelix.api.server.base.command;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.skin.SkinsApi;
import club.abelix.commons.data.skin.exception.SkinException;
import club.abelix.commons.data.skin.mojang.MojangApi;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.List;

@NamedCommand("clones")
public class ClonesCommand {

    @Executor
    @WithPermission("command.clones")
    public void execute(BukkitExecutionSession session) throws CommandException, SkinException {
        val start = session.getPlayer().getLocation();
        val count = session.getIntArgument(0, "count");
        val direction = BlockFace.valueOf(session.getArgument(1).toUpperCase());
        val skin = MojangApi.getSkinTextures(session.getArgument(2));
        val locations = generateLocations(start, count, direction);

        locations.forEach(location -> {
            val player = FalconAPI.getNmsApi().getFakeEntityApi().createPlayer(location, skin);
            player.setPublicStatus(true);
        });
    }

    private List<Location> generateLocations(Location startLocation, int count, BlockFace direction) {
        val locations = new ArrayList<Location>();

        for (int i = 0; i < count; i++) {
            val loc = startLocation.clone().add(direction.getModX() * i, direction.getModY() * i, direction.getModZ() * i);
            loc.setPitch(startLocation.getPitch());
            loc.setYaw(startLocation.getYaw());

            locations.add(loc);
        }

        return locations;
    }

}


