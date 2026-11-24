package club.abelix.api.base.report.gui.staff.action;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.language.Lang;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.packet.play.client.CReportAction;
import club.abelix.network.type.report.ReportReason;
import lombok.NonNull;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;

/**
 * @author pro0xy
 * <p>Created on 02.11.2023 at 20:58</p>
 */

public class ReportPunishmentGui extends Gui {

    private final PagedZone<ReportReason> zone;

    private static final ItemStack CLOSE_BUTTON = ItemUtil.getBuilder(Material.BARRIER)
            .setName(Lang.of("gui_close_menu_icon_name"))
            .build();

    public ReportPunishmentGui(@NonNull Gamer admin, @NonNull Gamer target) {
        super(admin.getPlayer(), Lang.of("report_punishment_gui_title")
                .set("%target%", target.getName())
                .compile(), 3);

        this.zone = new PagedZone<>(this) {
            @Override
            protected Collection<ReportReason> getItems() {
                return List.of(ReportReason.values());
            }

            @Override
            protected void drawItem(ReportReason reportReason, int slot) {
                ItemUtil.Builder item = ItemUtil.getBuilder(Material.WOOL);

                item.setName(getLanguage().getMessage("report_punishment_entry_name",
                        "%duration%", reportReason.getDurationString() == null ? "Permanent" : reportReason.getDurationString()
                        ));

                item.setLore("§f" + reportReason.getReason(getLanguage()));
                item.setDurability(DyeColor.RED.getWoolData());

                add(item.build(), slot, clickEvent -> {
                    Connection.sendPacket(
                            new CReportAction(
                                    CReportAction.Action.CLOSE,
                                    getGamer().getPlayerId(), target.getPlayerId(), reportReason
                            )
                    );

                    player.closeInventory();
                });
            }
        };

        this.zone.addSlotRect(1, 1, 7, 2);

        draw();
    }

    @Override
    public void draw(){

        add(CLOSE_BUTTON, toSlot(4, 0), clickEvent -> {
            player.closeInventory();
        });

        this.zone.draw();
    }
}
