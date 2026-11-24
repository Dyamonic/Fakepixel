package club.abelix.api.server.lobby.linkage.gui;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.api.server.lobby.linkage.LinkageRewardManager;
import club.abelix.commons.language.Lang;
import club.abelix.network.packet.play.both.BCheckChannelSubscription;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class LinkageRewardGui extends Gui {

    private static final ItemStack INFO_ITEM = ItemUtil.getBuilder(Material.SKULL_ITEM)
            .setDurability(3)
            .setSkullValue("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTY0MzlkMmUzMDZiMjI1NTE2YWE5YTZkMDA3YTdlNzVlZGQyZDUwMTVkMTEzYjQyZjQ0YmU2MmE1MTdlNTc0ZiJ9fX0=")
            .setName(Lang.of("linkage_reward_info_item_name"))
            .setLore(Lang.of("linkage_reward_info_item_lore"))
            .build();

    private static final ItemStack CLAIM_ITEM = ItemUtil.getBuilder(Material.STAINED_GLASS_PANE)
            .setDurability(5)
            .setName(Lang.of("linkage_reward_claim_item_name"))
            .setLore(Lang.of("linkage_reward_claim_item_lore"))
            .build();

    private final LinkageRewardManager linkageRewardManager;

    public LinkageRewardGui(final @NonNull Player player,
                            final @NonNull LinkageRewardManager linkageRewardManager,
                            final Gui parent) {
        super(player, Lang.of("linkage_reward_gui"), 3);

        setParent(parent);

        this.linkageRewardManager = linkageRewardManager;

        draw();
    }

    @Override
    public void draw() {
        add(CLAIM_ITEM, 12, e -> {
            if (linkageRewardManager.getWaitingPlayers().contains(gamer.getPlayerId())) {
                return;
            }

            player.closeInventory();

            if (gamer.getProperty("LinkageClaimed") == 1) {
                gamer.sendLocalizedMessage("linkage_reward_claim_already_claimed");

                gamer.playSound(YSound.NOTE_DIDGERIDOO, 1, 1.21f);
                return;
            }

            val authData = gamer.getAuthData();

            if (!authData.has2FA()) {
                gamer.sendLocalizedMessage("linkage_reward_claim_no_2fa");

                gamer.playSound(YSound.NOTE_DIDGERIDOO, 1, 1.21f);
                return;
            }

            val cooldownRecord = linkageRewardManager.checkAndSetCooldown(gamer);

            if (cooldownRecord != null) {
                gamer.sendLocalizedMessage("linkage_reward_claim_cooldown",
                        "%time%", cooldownRecord.remainingFormatted() + 1000L);
                gamer.playSound(YSound.NOTE_DIDGERIDOO, 1, 1.21f);
                return;
            }

            gamer.sendLocalizedMessage("linkage_reward_claim_check_subscription");

            gamer.playSound(YSound.ORB_PICKUP, 0.75f, 2.0f);

            linkageRewardManager.getWaitingPlayers().add(gamer.getPlayerId());

            val channel = linkageRewardManager.getChannelsToSubscribe().get(0);

            linkageRewardManager.checkChannelSubscription(gamer, channel).whenComplete((status, throwable) -> {
                linkageRewardManager.getWaitingPlayers().remove(gamer.getPlayerId());

                if (throwable != null) {
                    gamer.sendLocalizedMessage("linkage_reward_claim_error");

                    gamer.playSound(YSound.NOTE_DIDGERIDOO, 1, 1.21f);
                    return;
                }

                val isSubscribed = status == BCheckChannelSubscription.SubscriptionStatus.SUBSCRIBED;

                if (!isSubscribed) {
                    gamer.sendLocalizedMessage("linkage_reward_claim_" + status.name().toLowerCase());

                    gamer.playSound(YSound.NOTE_DIDGERIDOO, 1, 1.21f);
                    return;
                }

                gamer.setProperty("LinkageClaimed", 1);

                giveRewards(gamer);

                gamer.sendLocalizedMessage("linkage_reward_claim_success");
                gamer.playSound(YSound.LEVEL_UP);
            });
        });

        add(INFO_ITEM, 14, e -> {
            player.closeInventory();

            gamer.sendJsonMessage(language.getMessage("linkage_reward_info_message"));
        });

        if (getParent() != null) {
            generateBackItem(22, e -> {
                getParent().reopen();
                getParent().draw();
            });
        }
    }

    private void giveRewards(final @NonNull Gamer gamer) {
        gamer.addGold(50);
    }
}
