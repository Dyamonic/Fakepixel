package club.abelix.api.base.privategame;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.base.util.item.ModeIcons;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.commons.language.Language;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.packet.play.client.CGoPrivateGame;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.Mini;
import lombok.NonNull;
import lombok.val;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PrivateGameGui extends Gui {
    private static final int[] BASE_SLOTS = {
            20, 21, 22, 23, 24,
            29, 30, 31, 32, 33,
            38, 39, 40, 41, 42,
    };

    private static final int[] BORDER_SLOTS = {
            0, 9, 18, 27, 36, 45,       // Столбец 0
            8, 17, 26, 35, 44, 53,      // Столбец 8
            10, 11, 12, 13, 14, 15, 16  // Строка 1 (9-17)
    };

    private final List<Master> SUPPORTED_MASTERS = List.of(
            Master.BW,
            Master.ARCADE,
            Master.SW
    );

    private final List<Mini> SUPPORTED_MODES = List.of(
            Mini.BEDWARS_SOLO,
            Mini.BEDWARS_DOUBLES,
            Mini.BEDWARS_TRIO,
            Mini.BEDWARS_QUARDO,
            Mini.BOMB_LOBBERS,
            Mini.BLOCK_PARTY,
            Mini.TNT_TAG,
            Mini.TNT_RUN,
            Mini.SKYWARS_SOLO,
            Mini.SKYWARS_MINI,
            Mini.SKYWARS_TEAMS
    );

    private Master currentMaster = Master.BW;
    private int masterPage = 0;
    private final PagedZone<Mini> pagedZone;

    public PrivateGameGui(@NonNull Gamer gamer) {
        super(gamer.getPlayer(), gamer.getLanguage().getMessage("private_game_create_gui_title"), 6);

        if (gamer.getParty() == null || !gamer.getParty().isLeader(gamer)) {
            gamer.sendLocalizedMessage("private_game_not_party_or_leader");
            gamer.playSound(YSound.ANVIL_BREAK);
            gamer.getPlayer().closeInventory();
            this.pagedZone = null;
            return;
        }

        this.pagedZone = new PagedZone<Mini>(this) {
            @Override
            protected Collection<Mini> getItems() {
                return SUPPORTED_MODES.stream()
                        .filter(mini -> mini.getMaster() == currentMaster)
                        .collect(Collectors.toList());
            }

            @Override
            protected void drawItem(Mini mini, int slot) {
                Language lang = getLanguage();
                ItemStack item = ModeIcons.get(mini);
                add(ItemUtil.getBuilder(item)
                        .setName(lang.getMessage("private_game_item_name",
                                "%mode%", mini.getDisplayName()))
                        .setLore(lang.getMessageList("private_game_item_lore",
                                "%mode%", mini.getDisplayName()))
                        .build(), slot, e -> {
                    Connection.sendPacket(
                            new CGoPrivateGame(
                                    getGamer().getPlayerId(),
                                    mini
                            ));
                    getPlayer().closeInventory();
                });
            }
        };

        for (int slot : BASE_SLOTS) {
            this.pagedZone.addSlot(slot);
        }

        this.draw();
    }

    @Override
    public void draw() {
        removeRange(0, getSize());
        drawBorder(BORDER_SLOTS);
        Language lang = getLanguage();

        int pageSize = 5; // слоты 2, 3, 4, 5, 6
        int totalMasters = SUPPORTED_MASTERS.size();
        int maxPages = (int) Math.ceil((double) totalMasters / pageSize);

        if (masterPage > 0) {
            add(ItemUtil.getSkullBuilder("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjg0ZjU5NzEzMWJiZTI1ZGMwNThhZjg4OGNiMjk4MzFmNzk1OTliYzY3Yzk1YzgwMjkyNWNlNGFmYmEzMzJmYyJ9fX0=")
                    .setName(lang.getMessage("prev_page_name"))
                    .build(), 1, e -> {
                masterPage--;
                draw();
            });
        } else {
            remove(1);
        }

        if (masterPage < maxPages - 1) {
            add(ItemUtil.getSkullBuilder("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTYzMzlmZjJlNTM0MmJhMThiZGM0OGE5OWNjYTY1ZDEyM2NlNzgxZDg3ODI3MmY5ZDk2NGVhZDNiOGFkMzcwIn19fQ==")
                    .setName(lang.getMessage("next_page_name"))
                    .build(), 7, e -> {
                masterPage++;
                draw();
            });
        } else {
            remove(7);
        }

        int startIdx = masterPage * pageSize;
        for (int i = 0; i < pageSize; i++) {
            int slot = 2 + i;
            int masterIdx = startIdx + i;

            if (masterIdx < totalMasters) {
                Master master = SUPPORTED_MASTERS.get(masterIdx);
                ItemStack item = ModeIcons.get(master);
                val builder = ItemUtil.getBuilder(item)
                        .setName(lang.getMessage("private_game_master_name",
                                "%color%", (currentMaster == master ? "§a" : "§7"),
                                "%name%", master.getDisplayName()));

                if (currentMaster == master) {
                    builder.glowing();
                }

                add(builder.build(), slot, e -> {
                    if (currentMaster != master) {
                        currentMaster = master;
                        draw();
                    }
                });
            } else {
                remove(slot);
            }
        }

        pagedZone.draw();
        pagedZone.drawButtons(47, 51);

        add(Gui.CLOSE_MENU_ICON,
                49,
                e -> getPlayer().closeInventory());
    }
}


