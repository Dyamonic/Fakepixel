package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.ScopedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.data.rank.Rank;
import club.abelix.commons.data.rank.entity.RankEntity;
import club.abelix.commons.data.rank.service.RanksService;
import club.abelix.commons.data.rank.service.remote.RemoteRankService;
import club.abelix.commons.project.Project;
import club.abelix.commons.util.HastebinUtil;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.ranks.proto.PermissionUpdateRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@WithAlias("lp")
@WithAlias("perm")
@NamedCommand("permission")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class PermissionCommand {

    RanksService ranksService;

    @SubcommandExecutor("add")
    @WithCondition("minArgs: 2")
    @WithPermission("ranks.manage")
    public void add(CoreExecutionSession session) throws CommandTranslationException {
        val rank = resolveRank(session, 0);
        val permission = session.getArgument(1);

        ranksService.updatePermission(
                rank.getId(),
                permission,
                PermissionUpdateRequest.Action.ADD
        ).whenComplete((r, t) -> {
            if (t != null) {
                session.sendMessage("§cAn error occurred while adding the permission: " + t.getMessage());
                t.printStackTrace();
                return;
            }

            session.sendMessage("§aPermission '" + permission + "' added to rank " + rank.getName() + ".");
        });
    }

    @SubcommandExecutor("remove")
    @WithCondition("minArgs: 2")
    @WithPermission("ranks.manage")
    public void remove(CoreExecutionSession session) throws CommandTranslationException {
        val rank = resolveRank(session, 0);
        val permission = session.getArgument(1);

        ranksService.updatePermission(
                rank.getId(),
                permission,
                PermissionUpdateRequest.Action.REMOVE
        ).whenComplete((r, t) -> {
            if (t != null) {
                session.sendMessage("§cAn error occurred while removing the permission: " + t.getMessage());
                t.printStackTrace();
                return;
            }

            session.sendMessage("§aPermission '" + permission + "' removed from rank " + rank.getName() + ".");
        });
    }

    @SubcommandExecutor("list")
    @WithPermission("ranks.manage")
    public void list(CoreExecutionSession session) throws CommandTranslationException, IOException {
        if (session.getArgumentCount() == 0) {
            val ranks = session.getRanks().stream()
                    .map(r -> ranksService.findRawById(r.rank().getId()))
                    .map(Optional::orElseThrow)
                    .map(RemoteRankService.RankRecord::entity)
                    .toList();

            val tree = buildPermissionTree(ranks);

            HastebinUtil.paste(tree)
                    .thenAccept(url -> session.sendMessage(url));
            return;
        }

        val rank = resolveRank(session, 0);
        val rankEntity = ranksService.findRawById(rank.getId())
                .map(RemoteRankService.RankRecord::entity)
                .orElseThrow();

        val tree = buildPermissionTree(List.of(rankEntity));

        HastebinUtil.paste(tree)
                .thenAccept(url -> session.sendLocalizedMessage("rank_permission_list", "%url%", url));
    }

    @SubcommandExecutor("test")
    @WithCondition("minArgs: 1")
    @WithPermission("ranks.manage")
    public void test(CoreExecutionSession session) throws CommandTranslationException {
        val permission = session.getArgument(0);

        if (session.getArgumentCount() == 1) {
            boolean has = session.getSender().hasPermission(permission);
            session.sendMessage(has
                    ? "§aYou have the permission: " + permission
                    : "§cYou do not have the permission: " + permission);
            return;
        }

        val rank = resolveRank(session, 1);
        boolean has = rank.hasPermission(permission);

        session.sendMessage(has
                ? "§aRank " + rank.getName() + " has the permission: " + permission
                : "§cRank " + rank.getName() + " does not have the permission: " + permission);
    }

    private Rank resolveRank(CoreExecutionSession session, int index) throws CommandTranslationException {
        val rankArg = session.getArgument(index);
        val rankOptional = ranksService.findByName(rankArg.toLowerCase());

        if (rankOptional.isEmpty()) {
            throw new CommandTranslationException("rank_not_found");
        }

        return rankOptional.get();
    }

    private String buildPermissionTree(List<RankEntity> ranks) {
        StringBuilder sb = new StringBuilder();
        Set<Integer> visited = new HashSet<>();

        for (RankEntity rank : ranks) {
            appendRankTree(sb, rank, 0, visited);
        }

        return sb.isEmpty() ? "(No ranks found)" : sb.toString();
    }

    private void appendRankTree(StringBuilder sb, RankEntity rank, int depth, Set<Integer> visited) {
        if (!visited.add(rank.getId())) {
            sb.append("  ".repeat(depth))
                    .append("↳ [CYCLE DETECTED: ").append(rank.getName()).append("]\n");
            return;
        }

        sb.append("  ".repeat(depth))
                .append("• Rank: ").append(rank.getName())
                .append(" (priority=").append(rank.getPriority()).append(")\n");

        if (!rank.getPermissions().isEmpty()) {
            sb.append("  ".repeat(depth + 1)).append("Permissions:\n");
            for (String perm : rank.getPermissions()) {
                sb.append("  ".repeat(depth + 2)).append("- ").append(perm).append("\n");
            }
        }

        if (!rank.getInheritedRanks().isEmpty()) {
            sb.append("  ".repeat(depth + 1)).append("Inherits:\n");
            for (RankEntity inherited : rank.getInheritedRanks()) {
                appendRankTree(sb, inherited, depth + 2, visited);
            }
        }

        visited.remove(rank.getId());
    }
}
