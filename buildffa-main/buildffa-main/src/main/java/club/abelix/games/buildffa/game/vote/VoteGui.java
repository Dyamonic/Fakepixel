package club.abelix.games.buildffa.game.vote;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.language.Lang;
import club.abelix.commons.util.random.RandomUtil;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.config.WorldConfig;
import lombok.val;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class VoteGui extends Gui {
    private static final int INSTANT_CHANGE_COST = 15;
    private static final Collection<Integer> SLOTS = List.of(21, 23);

    private final BuildFFAGame game;

    public VoteGui(final BuildFFAGame game, Player player) {
        super(player, Lang.of("buildffa_map_selector_gui_title"), 6);

        this.game = game;

        draw();
    }

    @Override
    public void draw() {
        drawBorder(Gui.BORDER_SLOTS_FOR_SIX_ROWS_GUI);

        val voteManager = game.getVoteManager();
        val worlds = game.getFfaConfig().getGameWorlds();
        val iterator = RandomUtil.getRandomElements(worlds, Math.min(SLOTS.size(), worlds.size()), true)
                .stream().sorted(Comparator.comparing(WorldConfig::displayName))
                .iterator();

        for (val slot : SLOTS) {
            if (iterator.hasNext()) {
                val worldConfig = iterator.next();
                val item = createMapItem(
                        worldConfig,
                        voteManager.countVotes(worldConfig),
                        voteManager.getVote(gamer) == worldConfig
                );

                add(item, slot, event -> {
                    event.setCancelled(true);

                    val gamer = Gamer.getGamer((Player) event.getWhoClicked());

                    switch (event.getClick()) {
                        case LEFT -> {
                            voteManager.addVote(gamer, worldConfig);
                            gamer.sendLocalizedMessage("vote_success", "%name%", worldConfig.displayName());
                        }
                        case RIGHT -> {
                            if (game.getWorldConfig() == worldConfig) {
                                gamer.sendLocalizedMessage("buildffa_instant_change_same");
                                return;
                            }

                            if (!gamer.hasGold(INSTANT_CHANGE_COST)) {
                                gamer.sendLocalizedMessage("not_enough_golds_message");
                                return;
                            }

                            gamer.takeGold(INSTANT_CHANGE_COST);
                            voteManager.instantChange(gamer, game, worldConfig);
                            return;
                        }
                    }

                    draw();
                });
            }
        }
    }

    private ItemStack createMapItem(final WorldConfig config, int votes, boolean active) {
        val mapName = Lang.of("guivotes_map_name")
                .setAll("%map_name%", config.displayName())
                .compile();

        val lore = Lang.of("votes")
                .setAll("%count%", votes)
                .compile();

        return ItemUtil.getBuilder(Material.WOOL)
                .setName(mapName)
                .setLore(lore)
                .glowing(active)
                .setAmount(votes)
                .build();
    }
}
