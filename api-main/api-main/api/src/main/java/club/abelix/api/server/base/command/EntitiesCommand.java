package club.abelix.api.server.base.command;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.util.location.LocationUtil;
import club.abelix.api.nms.entity.type.FakeEntity;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.execution.Executor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@NamedCommand("entities")
public class EntitiesCommand {

    @Executor
    @WithPermission("command.entities")
    public static void doExecution(BukkitExecutionSession session) throws CommandException {
        List<FakeEntity> entities = new ArrayList<>(FalconAPI.getNmsApi().getFakeEntityApi().getTrackedEntities());
        int total = entities.size();

        double distance = -1;

        if (session.getArgumentCount() > 1) {
            distance = session.getIntArgument(1, "distance");
        }

        if (distance >= 0) {
            Location playerLocation = session.getPlayer().getLocation();

            double finalDistance = distance;
            entities.removeIf(entity -> !LocationUtil.isNearby(playerLocation, entity.getLocation(), finalDistance));
        }

        if (session.getArgumentCount() > 2) {
            String filteredName = session.getArgument(2);

            entities.removeIf(entity -> !entity.getReceivers().stream().map(Player::getName)
                    .collect(Collectors.toSet()).contains(filteredName));
        }

        entities.sort(Comparator.comparing(FakeEntity::getEntityId).thenComparing(FakeEntity::getEntityType));

        int page = 1;
        final int perPage = 5;

        if (session.getArgumentCount() > 0) {
            page = session.getIntArgument(0, "ID");
        }

        int pages = entities.size() / perPage;

        if (pages == 0) {
            return;
        }

        if (page > pages) {
            page = pages;
        }

        int sent = 0;
        int skip = (page - 1) * perPage;

        session.sendMessage("Total entities: §c" + total);
        session.sendMessage("Entities (§c" + page + "§f/§c" + pages + "§f)");

        for (FakeEntity entity : entities) {
            if (skip-- > 0) {
                continue;
            }

            session.sendMessage(" - §c#" + entity.getEntityId() + "§f, §c"
                    + entity.getClass().getSimpleName() + " §c" + entity.getCustomName() + " §fat §c" + LocationUtil.locationToString(entity.getLocation(), false));
            session.sendMessage("   - Receivers: §c" + wrap(entity.getReceivers()));
            session.sendMessage("   - Viewers: §c" + wrap(entity.getViewers()));

            if (sent++ > perPage) {
                break;
            }
        }
    }

    public static List<String> wrap(Collection<? extends Player> players) {
        return players.stream().map(Player::getName).sorted().collect(Collectors.toList());
    }

}