package club.abelix.api.base.report.gui.player;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.language.Lang;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.packet.play.client.CReportAction;
import club.abelix.network.type.report.ReportReason;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ReportGui extends Gui {

    private final Gamer target;
    private final PagedZone<ReportReason> zone;

    private static final ItemStack CLOSE_BUTTON = ItemUtil.getBuilder(Material.BARRIER)
            .setName(Lang.of("gui_close_menu_icon_name"))
            .build();

    public ReportGui(@NonNull Gamer gamer, @NonNull Gamer target) {
        super(
                gamer.getPlayer(),
                Lang.of("report_gui_title")
                        .set("%target%", target.getName())
                        .compile(),
                5
        );

        this.target = target;
        this.zone = new PagedZone<>(this) {
            @Override
            protected Collection<ReportReason> getItems() {
                return List.of(ReportReason.values());
            }

            @Override
            protected void drawItem(ReportReason reportReason, int slot) {
                ItemUtil.Builder builder = ItemUtil.getBuilder(Material.PAPER);
                builder.setName("§e" + reportReason.getReason(getLanguage()));

                builder.setLore(getLanguage().getMessage("report_gui_item_lore"));

                add(builder.build(), slot, clickEvent -> {
                    player.closeInventory();

                    //отправляем пакет на кор
                    Connection.sendPacket(
                            new CReportAction(
                                    CReportAction.Action.SEND,
                                    getGamer().getPlayerId(), target.getPlayerId(), reportReason
                            )
                    );
                });
            }
        };

        this.zone.addSlotRect(1, 1, 7, 3);

        draw();
    }

    @Override
    public void draw() {
        add(CLOSE_BUTTON, toSlot(4, 4), clickEvent -> player.closeInventory());

        this.zone.draw();
    }
}
