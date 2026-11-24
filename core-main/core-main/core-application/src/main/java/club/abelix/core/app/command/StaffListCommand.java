package club.abelix.core.app.command;

import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.rank.Rank;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;

import java.util.*;

@WithAlias("stafflist")
@WithAlias("staff")
@IgnoreLogs
@NamedCommand("staffs")
public final class StaffListCommand {

    @Executor
    public static void execute(CoreExecutionSession session) {
        Map<Rank, List<String>> staff = new HashMap<>();
        List<String> hiddenNames = new ArrayList<>();

        for (Gamer gamer : Gamer.getGamers()) {
            if (!gamer.getRanks().isStaff())
                continue;

            if (gamer.isVanished()) {
                hiddenNames.add(gamer.getDisplayName());
                continue;
            }

            Rank priorityRank = gamer.getRanks().findPriorityRank().rank();
            staff.computeIfAbsent(priorityRank, k -> new ArrayList<>())
                    .add(gamer.getDisplayName());
        }

        int count = staff.values().stream().mapToInt(List::size).sum();
        boolean canSeeHiddenPlayers = session.isConsole() || session.getGamer().canSeeVanish();

        if (count == 0 && (!canSeeHiddenPlayers || hiddenNames.isEmpty())) {
            session.sendLocalizedMessage("core_no_online_staff");
            return;
        }

        session.sendLocalizedMessage("core_stafflist_result", "%count%", count);

        staff.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> -entry.getKey().getPriority()))
                .forEach(entry -> {
                    session.sendLocalizedMessage("core_stafflist_in_group",
                            "%group%", entry.getKey().getDisplayName(session.getLanguage()));
                    entry.getValue().forEach(player -> session.sendMessage(" - " + player));
                });

        if (canSeeHiddenPlayers && !hiddenNames.isEmpty()) {
            session.sendLocalizedMessage("core_stafflist_vanished",
                    "%names%", String.join("§f, ", hiddenNames));
        }
    }
}
