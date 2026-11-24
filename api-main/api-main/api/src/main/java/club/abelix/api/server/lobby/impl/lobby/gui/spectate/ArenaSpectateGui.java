package club.abelix.api.server.lobby.impl.lobby.gui.spectate;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.base.util.item.ModeIcons;
import club.abelix.commons.language.Lang;
import club.abelix.core.bukkit.StarshipBukkit;
import club.abelix.network.common.enums.GameState;
import club.abelix.network.online.data.FieldMatchMode;
import club.abelix.network.online.data.ServerKind;
import club.abelix.network.online.field.StandardServerFields;
import club.abelix.network.online.query.StatusQueries;
import club.abelix.network.online.query.StatusQueryResult;
import club.abelix.network.type.mode.Mini;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ArenaSpectateGui extends Gui {

    private static final ItemStack NO_PERMISSION_ICON = ItemUtil.getBuilder(Material.BARRIER)
            .setName(Lang.of("arena_spectate_gui_no_permission_icon"))
            .setLore(Lang.of("arena_spectate_gui_no_permission_icon_lore"))
            .build();

    private static final ItemStack NO_STARTED_ARENAS_ICON = ItemUtil.getBuilder(Material.GLASS_BOTTLE)
            .setName(Lang.of("arena_spectate_gui_no_arenas_icon"))
            .setLore(Lang.of("arena_spectate_gui_no_arenas_icon_lore"))
            .build();

    PagedZone<SpectateArena> zone;
    Set<Mini> minis;
    Map<String, SpectateArena> arenas;

    @NonFinal
    StatusQueryResult result;

    public ArenaSpectateGui(
            final @NotNull Player player,
            final @NotNull Set<Mini> minis
    ) {
        super(player, Lang.of("arena_spectate_gui_title"), 5);

        this.minis = minis.stream()
                .filter(m -> m != Mini.BEDWARS_RANKED)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        this.arenas = new HashMap<>();
        this.lookup();

        this.zone = new PagedZone<>(this) {

            @Override
            protected Collection<SpectateArena> getItems() {
                return arenas.values();
            }

            @Override
            protected void drawItem(SpectateArena arena, int slot) {
                Mini mini = arena.getMini();

                String server = arena.getServer();

                ItemUtil.Builder builder = ItemUtil.getBuilder(ModeIcons.get(mini));
                builder.setName(
                        translated("arena_spectate_gui_entry",
                                "%mini%", mini.getDisplayName()
                        )
                );
                builder.setLore(
                        translatedLore("arena_spectate_gui_entry_lore",
                                "%map%", translated(arena.getMap()),
                                "%server%", arena.getServer(),
                                "%players%", arena.getPlayers()
                        )
                );
                builder.addItemFlags();

                add(builder.build(), slot, e -> {
                    getGamer().sendLocalizedMessage("server_teleportation_started",
                            "%server_name%", server);

                    getGamer().connectExact(server);
                });
            }

        };

        this.zone.addSlotRect(1, 1, 7, 3);

        draw();

        repaintAfter(1);
    }

    private void lookup() {
        val query = StatusQueries.parametrizedQuery()
                .arenaState(GameState.GAME)
                .modes(minis.stream().map(Enum::name).toList())
                .kind(ServerKind.ARENA)
                .build();

        val result = StarshipBukkit.getInstance()
                .getStatusQueryExecutionService()
                .executeSync(query);

        if (result.isEmpty()) {
            gamer.sendLocalizedMessage("no_servers_available");
            return;
        }

        this.result = result;
    }

    @Override
    public void draw() {
        Gamer gamer = getGamer();

        if (!gamer.getRanks().hasPermission("game.spectate")) {
            add(NO_PERMISSION_ICON, toSlot(4, 2));
            return;
        }

        for (val server : result.getPerServer().values()) {
            if (!(server instanceof StatusQueryResult.ArenaPerServer arenaPerServer)) continue;

            if (GameState.valueOf(arenaPerServer.getArenaState().toUpperCase()) != GameState.GAME) {
                continue;
            }

            Mini mini = Mini.valueOf(arenaPerServer.getMode().toUpperCase());

            arenas.put(
                    arenaPerServer.getName(),
                    new SpectateArena(arenaPerServer.getName(), arenaPerServer.getMapName(), mini, arenaPerServer.getOnline())
            );
        }

        if (arenas.isEmpty()) {
            add(NO_STARTED_ARENAS_ICON, toSlot(4, 2));
            return;
        }

        zone.draw();
        zone.drawButtons(toSlot(0, 4), toSlot(8, 4));
    }
}
