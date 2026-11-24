package club.abelix.api.server.base.command;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.util.location.LocationUtil;
import club.abelix.api.nms.entity.type.FakeEntity;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.SubcommandExecutor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@NamedCommand("entity")
public class EntityCommand {

    @WithCondition("minArgs: 1")
    @WithPermission("command.entities")
    @SubcommandExecutor("get")
    public static void getEntity(BukkitExecutionSession session) throws CommandException {
        int entityId = session.getIntArgument(0, "entity_id");

        FakeEntity entity = FalconAPI.getNmsApi().getFakeEntityApi().getFakeEntity(entityId);

        EntityType type;

        String name;
        Location location;

        boolean publicStatus;

        boolean fake;

        if (entity == null) {
            Entity bukkitEntity = FalconAPI.getNmsApi().getWorldApi().getCraftEntityById(session.getPlayer().getWorld(), entityId);

            if (bukkitEntity == null) {
                throw new CommandTranslationException("entity_not_found", "%id%", entityId);
            }

            name = bukkitEntity.getCustomName();
            location = bukkitEntity.getLocation();
            type = bukkitEntity.getType();
            publicStatus = true;
            fake = false;
        } else {
            String customName = entity.getCustomName();

            name = customName == null ? "-" : customName;
            location = entity.getLocation();
            type = entity.getEntityType();
            publicStatus = entity.isPublicStatus();
            fake = true;
        }

        session.sendLocalizedMessage("entity_get_result",
                "%id%", entityId, "%name%", name,
                "%world%", location.getWorld().getName(), "%x%", location.getX(),
                "%y%", location.getY(), "%z%", location.getZ(),
                "%public%", publicStatus, "%type%", type,
                "%fake%", fake);
    }

    @WithPermission("command.entities.list")
    @SubcommandExecutor("list")
    public static void listEntities(BukkitExecutionSession session) throws CommandException {
        List<FakeEntity> entities = new ArrayList<>(FalconAPI.getNmsApi().getFakeEntityApi().getTrackedEntities());

        if (session.getArgumentCount() > 1) {
            int distance = session.getIntArgument(1, "distance");

            Location playerLocation = session.getPlayer().getLocation();
            entities.removeIf(entity -> !LocationUtil.isNearby(playerLocation, entity.getLocation(), distance));
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
