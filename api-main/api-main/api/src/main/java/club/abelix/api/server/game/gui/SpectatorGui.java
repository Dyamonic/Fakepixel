package club.abelix.api.server.game.gui;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.server.game.Game;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

public class SpectatorGui extends Gui {

    private final PagedZone<Player> zone;

    public SpectatorGui(@NonNull Game game, @NonNull Gamer gamer) {
        super(gamer.getPlayer(), gamer.getLanguage()
                .getMessage("game_spectator_gui_title"), 3);

        this.zone = new PagedZone<Player>(this) {

            @Override
            protected Collection<Player> getItems() {
                return game.getAlivePlayers()
                        .stream()
                        .map(Gamer::getPlayer)
                        .sorted(Comparator.comparing(Player::getName))
                        .collect(Collectors.toList());
            }

            @Override
            protected void drawItem(Player alive, int slot) {
                Gamer aliveGamer = Gamer.getGamer(alive);

                // Отрисовка иконки игрока
                ItemStack skull = ItemUtil.getBuilder(aliveGamer.getSkull())
                        .setName("§b" + aliveGamer.getDisplayName())
                        .addLore("")
                        .addLore(gamer.getLanguage().getMessage("spectator_gui_teleport"))
                        .build();

                add(skull, slot, e -> {
                    if (e.isLeftClick()) {
                        if (alive.isOnline()) {
                            Player player = gamer.getPlayer();

                            player.closeInventory();

                            player.setAllowFlight(true);
                            player.setFlying(true);

                            player.teleport(alive.getLocation());
                        } else {
                            draw();
                        }
                    }
                });
            }

        };

        this.zone.addSlotRect(0, 0, 9, 2);
        this.zone.addSlotRect(1, 2, 7, 1);

        draw();
        repaintAfter(2);
    }

    @Override
    public void draw() {
        this.zone.draw();
        this.zone.drawButtons(toSlot(0, 2), toSlot(8, 2));
    }

}
