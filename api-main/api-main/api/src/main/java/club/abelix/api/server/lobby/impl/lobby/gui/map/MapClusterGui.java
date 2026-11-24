package club.abelix.api.server.lobby.impl.lobby.gui.map;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.commons.language.Lang;
import club.abelix.core.bukkit.StarshipBukkit;
import club.abelix.network.common.enums.GameState;
import club.abelix.network.online.data.ServerKind;
import club.abelix.network.online.query.StatusQueries;
import club.abelix.network.online.query.StatusQueryResult;
import club.abelix.network.type.mode.Mini;
import lombok.val;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class MapClusterGui extends Gui {

    private final Mini mini;
    private final PagedZone<MapCluster> zone;
    private final Map<String, MapCluster> clusters = Collections.synchronizedMap(new HashMap<>());
    private final Gui prev;
    private int standByServers = 0;

    public MapClusterGui(Gui prev, Gamer gamer, Mini mini) {
        super(gamer.getPlayer(), mini.getMaster().getDisplayName() + " " + mini.getDisplayName(), 6);

        this.mini = mini;
        this.prev = prev;

        this.lookup();

        this.zone = new PagedZone<>(this) {
            @Override
            protected Collection<MapCluster> getItems() {
                return new ArrayList<>(clusters.values());
            }

            @Override
            protected void drawItem(MapCluster cluster, int slot) {
                ItemStack item = ItemUtil.getBuilder(Material.PAPER)
                        .setName(translated("map_cluster_gui_entry", "%game_server_name%", cluster.getServerName(),
                                "%game_server_map%", translated(cluster.getMap())))
                        .setLore(translatedLore("map_cluster_gui_entry_lore",
                                "%game_type%", ChatColor.stripColor(cluster.getMini().getShortName()),
                                "%available_games%", cluster.getAvailableGames()))
                        .addItemFlags()
                        .build();

                add(item, slot, e -> {
                    if (gamer.getRanks().hasPermission("map.select")) {
                        getGamer().connectMap(cluster.getMini(), cluster.getMap());
                    } else {
                        gamer.playSound(YSound.VILLAGER_NO);
                        gamer.sendLocalizedMessage("map_select_unavailable_no_permission");
                    }
                });
            }

        };

        this.zone.addSlotRect(1, 1, 7, 3);

        draw();
    }

    @Override
    public void draw() {
        zone.draw();

        drawBaseItems();

        if (prev != null) {
            generateBackItem(ItemUtil.getBuilder(Material.ARROW)
                    .setName(Lang.of("gui_back_to_menu_icon_name"))
                    .setLore(Lang.of("gui_back_to_menu_icon_lore"))
                    .build(), toSlot(5, 5), e -> prev.reopen());
        }
    }

    private void drawBaseItems() {
        add(ItemUtil.getBuilder(Material.FIREWORK)
                .setName(translated("map_cluster_gui_best"))
                .setLore(translatedLore("map_cluster_gui_best_lore",
                        "%game_type_mini_name%", ChatColor.stripColor(mini.getShortName()),
                        "%free%", standByServers))
                .build(), toSlot(3, 5), e -> getGamer().connectMode(mini));
    }

    private void lookup() {
        val query = StatusQueries.parametrizedQuery()
                .arenaState(GameState.STAND_BY)
                .mode(mini.name())
                .kind(ServerKind.ARENA)
                .build();

        val result = StarshipBukkit.getInstance().getStatusQueryExecutionService().executeSync(query);

        for (val entry : result.getPerServer().entrySet()) {
            val server = entry.getValue();

            if (!(server instanceof StatusQueryResult.ArenaPerServer arena)) {
                continue;
            }

            MapCluster cluster = clusters.get(arena.getMapName());

            if (cluster == null) {
                if (arena.getArenaState().equals(GameState.STAND_BY.name())) {
                    standByServers++;
                }

                int standbyCount = result.getLookedUpServers().size();

                cluster = new MapCluster(
                        server.getName(),
                        arena.getMapName(),
                        Mini.valueOf(arena.getMode().toUpperCase()),
                        GameState.valueOf(arena.getArenaState().toUpperCase()),
                        arena.getOnline(),
                        arena.getSlots(),
                        standbyCount
                );

                clusters.put(arena.getMapName(), cluster);
            }
        }
    }
}
