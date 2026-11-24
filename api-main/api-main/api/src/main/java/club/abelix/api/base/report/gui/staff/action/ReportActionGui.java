package club.abelix.api.base.report.gui.staff.action;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.report.gui.staff.ReportListGui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.language.Lang;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.packet.play.client.CReportAction;
import lombok.NonNull;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * @author pro0xy
 * <p>Created on 02.11.2023 at 20:33</p>
 */

public class ReportActionGui extends Gui {

    private final Gamer target;
    private final ReportListGui.Report report;

    private static final ItemStack CLOSE_REPORT = ItemUtil.getBuilder(Material.WOOL)
            .setDurability(DyeColor.GREEN.getWoolData())
            .setName(Lang.of("report_action_close_report_icon_name"))
            .setLore(Lang.of("report_action_close_report_icon_lore"))
            .build();

    private static final ItemStack SPECTATE = ItemUtil.getBuilder(Material.WOOL)
            .setName(Lang.of("report_action_spectate_icon_name"))
            .setLore(Lang.of("report_action_spectate_icon_lore"))
            .build();

//    private static final ItemStack ISSUE_PUNISHMENT = ItemUtil.getBuilder(Material.WOOL)
//            .setDurability(DyeColor.RED.getWoolData())
//            .setName(Lang.of("report_action_issue_punishment_icon_name"))
//            .setLore(Lang.of("report_action_issue_punishment_icon_lore"))
//            .build();

    private static final ItemStack CLOSE_BUTTON = ItemUtil.getBuilder(Material.BARRIER)
            .setName(Lang.of("gui_close_menu_icon_name"))
            .build();

    public ReportActionGui(@NonNull Gamer admin, @NonNull Gamer target, @NonNull ReportListGui.Report report, Gui previousGui) {
        super(admin.getPlayer(), Lang.of("report_action_gui_title")
                .set("%target%", target.getName())
                .compile(), 6);

        this.target = target;
        this.report = report;

        if (previousGui != null) {
            generateBackItem(toSlot(4, 5), clickEvent -> previousGui.reopen());
        } else {
            add(CLOSE_BUTTON, toSlot(4, 5), clickEvent -> player.closeInventory());
        }

        draw();
    }

    @Override
    public void draw() {
        drawInformationAboutReport();
        drawActionButtons();
    }

    private void drawInformationAboutReport(){
        ItemUtil.Builder itemBuilder = ItemUtil.getBuilder(target.getSkull());

        itemBuilder.setName(target.getDisplayName());

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

        add(itemBuilder.build(), toSlot(4, 1));
    }

    private void drawActionButtons() {
        add(CLOSE_REPORT, toSlot(3, 3), clickEvent -> {
            Connection.sendPacket(new CReportAction(CReportAction.Action.CLOSE, getGamer().getPlayerId(), target.getPlayerId(), null));
            player.closeInventory();
            getGamer().sendLocalizedMessage("report_action_close_report_message", "%target%", target.getDisplayName());
        });

        add(SPECTATE, toSlot(5, 3), clickEvent -> {
            Connection.startReportSpectating(getGamer().getPlayerId(), getGamer().getPlayerId());
            player.closeInventory();
            getGamer().connectToPlayer(target);
        });

//        add(ISSUE_PUNISHMENT, toSlot(6, 3), clickEvent -> {
//            new ReportPunishmentGui(getGamer(), target);
//        });
    }
}
