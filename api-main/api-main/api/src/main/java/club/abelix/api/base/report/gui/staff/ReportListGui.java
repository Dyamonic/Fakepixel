package club.abelix.api.base.report.gui.staff;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.report.gui.staff.action.ReportActionGui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.language.Lang;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.packet.play.client.CGetReports;
import club.abelix.network.type.report.PlayerReport;
import club.abelix.network.type.report.PlayerReportSender;
import club.abelix.network.type.report.ReportReason;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportListGui extends Gui {

    /**
     * Иконка о том, что жалоб нет
     */
    private static final ItemStack NO_REPORTS_ITEM = ItemUtil.getBuilder(Material.BUCKET)
            .setName(Lang.of("report_not_found_icon"))
            .setLore(Lang.of("report_not_found_icon_lore"))
            .build();

    private final PagedZone<Report> zone;

    /**
     * Список репортов
     */
    private final List<Report> reports = new ArrayList<>();

    public ReportListGui(@NonNull Player player) {
        super(player, Lang.of("report_list_gui_title"), 6);

        this.zone = new PagedZone<>(this) {

            @Override
            protected Collection<Report> getItems() {
                return reports;
            }

            @Override
            protected void drawItem(Report report, int slot) {
                Gamer gamer = report.getRuleBreaker();

                if (gamer == null) {
                    return;
                }

                ItemUtil.Builder itemBuilder = ItemUtil.getBuilder(gamer.getSkull());

                itemBuilder.setName(gamer.getDisplayName());

                itemBuilder.setLore(translatedLore(
                        "report_list_icon_lore",
                        "%server%", report.getRuleBreakerServer(),
                        "%count%", report.getSenderMap().size())
                );

                report.getSenderMap().forEach((sender, reason) -> itemBuilder.addLore(translatedLore(
                        "report_list_icon_lore_player",
                        "%name%", sender.getDisplayName(),
                        "%reason%", reason.getReason(getLanguage())
                )));

                itemBuilder.addLore(translatedLore("report_list_icon_lore_actions"));

                add(itemBuilder.build(), slot, clickEvent -> {
                    Gamer admin = getGamer();

                    new ReportActionGui(admin, gamer, report, ReportListGui.this);
                });
            }

        };

        this.zone.addSlotRect(1, 1, 7, 4);
        this.draw();
    }

    @Override
    public void draw() {
        val gamer = getGamer();

        if (!gamer.getRanks().hasPermission("report.view")) {
            gamer.sendLocalizedMessage("dont_have_permission");
            getPlayer().closeInventory();

            return;
        }

        add(DATA_IS_LOADING_ICON, toSlot(4, 2));

        loadReportList(() -> {
            if (reports.isEmpty()) {
                add(NO_REPORTS_ITEM, toSlot(4, 2));
                return;
            }

            remove(toSlot(4, 2));

            zone.draw();
            zone.drawButtons(48, 50);
        });
    }

    private void loadReportList(Runnable complete) {
        Connection.awaitPacket(new CGetReports())
                .whenComplete((response, cause) -> {
                    for (PlayerReport report : response.getReports()) {
                        Gamer gamer = Gamer.getOrCreate(report.getPlayerId(), loadedGamer -> {
                            loadedGamer.loadPlayerId();
                            loadedGamer.loadRanks();
                            loadedGamer.loadSkull();
                            loadedGamer.updateDisplayName();
                        });

                        String serverName = report.getServer();

                        Map<Gamer, ReportReason> senderMap = new HashMap<>();

                        for (PlayerReportSender reportSender : report.getSenders()) {
                            Gamer sender = Gamer.getOrCreate(reportSender.getPlayerId(), loadedGamer -> {
                                loadedGamer.loadPlayerId();
                                loadedGamer.loadRanks();
                                loadedGamer.loadSkull();
                                loadedGamer.updateDisplayName();
                            });

                            senderMap.put(sender, reportSender.getReason());
                        }

                        if (senderMap.size() >= 2) {
                            reports.add(new Report(gamer, serverName, senderMap));
                        }
                    }

                    reports.sort(Comparator
                            .comparingInt((Report report) -> report.getRankSortWeight())
                            .thenComparing(report -> report.getRuleBreaker().getName(), String.CASE_INSENSITIVE_ORDER));

                    complete.run();
                });
    }

    @Getter
    @RequiredArgsConstructor
    public static class Report {

        private final Gamer ruleBreaker;

        private final String ruleBreakerServer;

        private final Map<Gamer, ReportReason> senderMap;

        public int getRankSortWeight() {
            if (ruleBreaker == null || ruleBreaker.getRanks() == null || ruleBreaker.getRanks().findPriorityRank() == null) {
                return 0;
            }

            return ruleBreaker.getRanks().findPriorityRank().rank().getId();
        }
    }
}
