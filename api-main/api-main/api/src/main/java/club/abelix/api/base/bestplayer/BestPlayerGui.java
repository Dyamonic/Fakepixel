package club.abelix.api.base.bestplayer;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.ClickAction;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.data.bestplayer.BestPlayerDonationEntry;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.time.Duration;
import club.abelix.commons.util.word.CommonWords;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.network.common.logging.Logging;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BestPlayerGui extends Gui {

    private final BestPlayerManager manager;

    public BestPlayerGui(@NonNull Player player, @NonNull BestPlayerManager manager) {
        super(player, "lang:best_player_gui_title;", 3);

        this.manager = manager;

        draw();
    }

    @Override
    public void draw() {
        BestPlayer bestPlayer = manager.getCurrentBestPlayer();

        Gamer bestGamer = bestPlayer.getGamer();
        Gamer gamer = getGamer();

        if (bestGamer == null || gamer == null) {
            return;
        }

        Language language = getLanguage();

        ItemUtil.Builder skullBuilder = ItemUtil.getBuilder(bestGamer.getSkull())
                .setName(bestGamer.getDisplayName());

        ItemUtil.Builder donateBuilder = ItemUtil.getBuilder(Material.EMERALD)
                .setName(translated("best_player_donate_name"));

        if (bestGamer.getPlayerId() == gamer.getPlayerId()) {
            add(skullBuilder.addLore(translatedLore("best_player_self_lore")).build(), toSlot(3, 1));

            donateBuilder.addLore(translatedLore("best_player_donate_self_lore"));

            if (bestPlayer.getDonateList().isEmpty()) {
                donateBuilder.addLore(translated("best_player_self_no_donates"));
            } else {
                donateBuilder.addLore(translated("best_player_self_last_donates"));

                List<BestPlayerDonationEntry> donaterList = new ArrayList<>(bestPlayer.getDonateList());
                donaterList.sort(Comparator.comparingLong(BestPlayerDonationEntry::time));
                donaterList = donaterList.subList(0, Math.min(10, donaterList.size()));

                Collections.reverse(donaterList);

                for (BestPlayerDonationEntry donation : donaterList) {
                    Gamer donater = Gamer.getOrCreate(donation.donaterId(), loadedGamer -> {
                        loadedGamer.loadPlayerId();
                        loadedGamer.loadRanks();
                        loadedGamer.loadPrefix();
                    });

                    if (donater == null) {
                        continue;
                    }

                    donateBuilder.addLore(
                            translatedLore(
                                    "best_player_self_donate_row",
                                    "%name%", donater.getDisplayName(),
                                    "%time%", Duration.durationWordsMs(language, System.currentTimeMillis() - donation.time())
                            )
                    );
                }
            }

            add(donateBuilder.build(), toSlot(5, 1));
        } else {
            int price = bestPlayer.getPrice();
            int golds = getGamer().getGold();

            ClickAction purchaseAction = null;

            skullBuilder.addLore(translatedLore("best_player_purchase_lore"));

            if (!bestPlayer.canBuy()) {
                skullBuilder.addLore(
                        translatedLore(
                                "best_player_purchase_unavailable_time_lore",
                                "%time%", Duration.durationWordsMs(language, bestPlayer.getTimeBeforeBuy())
                        )
                );
            } else {
                if (price > golds) {
                    skullBuilder.addLore(
                            translated(
                                    "best_player_purchase_unavailable_golds_lore",
                                    "%cost%", CommonWords.GOLD.get(language).withNumber(price - golds)
                            )
                    );
                } else {
                    skullBuilder.addLore(
                            translated(
                                    "best_player_purchase_available_lore",
                                    "%cost%", CommonWords.GOLD.get(language).withNumber(price)
                            )
                    );

                    purchaseAction = e -> {
                        purchase();
                        getPlayer().closeInventory();
                    };
                }
            }

            add(skullBuilder.build(), toSlot(3, 1), purchaseAction);

            ClickAction donateAction = null;

            donateBuilder.addLore(
                    translatedLore("best_player_donate_lore", "%cost%",
                            CommonWords.GOLD.get(language).withNumber(BestPlayer.BASE_COST))
            );

            boolean donationsAllowed = bestPlayer.isDonationsAllowed();

            if (!donationsAllowed || !bestPlayer.canDonate(gamer)) {
                donateBuilder.addLore(
                        donationsAllowed
                                ? translatedLore("best_player_donate_unavailable_already_lore", "%name%", bestGamer.getDisplayName())
                                : translatedLore("best_player_donate_unavailable_unknown_lore")
                );
            } else if (golds < BestPlayer.DONATE_SUM) {
                donateBuilder.addLore(
                        translatedLore("best_player_donate_unavailable_golds_lore",
                                "%name%", bestGamer.getDisplayName(),
                                "%sum%", CommonWords.GOLD.get(language).withNumber(BestPlayer.DONATE_SUM))
                );
            } else {
                donateBuilder.addLore(
                        translated(
                                "best_player_donate_available",
                                "%sum%", CommonWords.GOLD.get(language).withNumber(BestPlayer.DONATE_SUM)
                        )
                );

                donateAction = e -> {
                    HeartBeat.runAsync(this::donate);

                    getPlayer().closeInventory();
                };
            }

            add(donateBuilder.build(), toSlot(5, 1), donateAction);
        }

        repaintAfter(1);
    }

    private void purchase() {
        BestPlayer bestPlayer = manager.getCurrentBestPlayer();

        Gamer gamer = getGamer();

        if (gamer.hasPermission("core.staff")) {
            gamer.sendLocalizedMessage("best_player_staff_unavailable");
            return;
        }

        int golds = getGamer().getGold();
        int price = bestPlayer.getPrice();

        if (golds < price) {
            gamer.sendLocalizedMessage("best_player_purchase_unavailable_golds");
            return;
        }

        gamer.takeGold(price);
        manager.update(gamer, price);

        gamer.sendLocalizedMessage("best_player_purchase_success");
    }

    private void donate() {
        BestPlayer bestPlayer = manager.getCurrentBestPlayer();

        Gamer bestPlayerGamer = bestPlayer.getGamer();
        Gamer gamer = getGamer();

        Language language = getLanguage();

        int golds = gamer.getGold();

        if (golds < BestPlayer.DONATE_SUM) {
            gamer.sendLocalizedMessage("best_player_donate_unavailable_golds", "%name%", bestPlayerGamer.getDisplayName());
            return;
        }

        if (!bestPlayer.canDonate(gamer)) {
            gamer.sendLocalizedMessage("best_player_donate_unavailable_already");
            return;
        }

        gamer.takeGold(BestPlayer.DONATE_SUM);
        manager.addDonation(gamer);

        gamer.sendLocalizedMessage("best_player_donate", "%sum%", CommonWords.GOLD.get(language).withNumber(BestPlayer.DONATE_SUM));
    }

}
