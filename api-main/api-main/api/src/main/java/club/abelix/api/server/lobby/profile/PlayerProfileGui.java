package club.abelix.api.server.lobby.profile;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.party.Party;
import club.abelix.api.base.util.item.Heads;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.base.util.skin.SkinUtil;
import club.abelix.commons.data.level.Leveling;
import club.abelix.commons.data.setting.Setting;
import club.abelix.commons.data.setting.media.SocialMediaType;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.data.skin.SkinsApi;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.time.Duration;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import club.abelix.network.common.NetworkPlayerLoad;
import club.abelix.network.packet.play.client.CNetworkPlayerLoadRequest;
import club.abelix.network.packet.play.client.CPlayerCommand;
import lombok.Getter;
import lombok.NonNull;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PlayerProfileGui extends Gui {

    private final PlayerProfileView targetProfile;

    public PlayerProfileGui(@NonNull Player player, @NonNull Gamer gamer) {
        super(player, "lang:profile_gui_title:%name%:" + gamer.getName() + ";", 5);

        this.targetProfile = new PlayerProfileView(gamer);

        draw();
    }

    @Override
    public void draw() {
        int loadingIconSlot = toSlot(4, 2);

        HeartBeat.runAsync(() -> {
            targetProfile.loadPlayerInfo();

            targetProfile.loadNetworkInfo().whenComplete((response, cause) -> {
                drawPlayerSkull();
                drawPartyItem();
            });

            drawSocialMedia(SocialMediaType.DISCORD, Heads.Discord.getItem(), 1, 1);
            drawSocialMedia(SocialMediaType.TELEGRAM, Heads.Social_Skype.getItem(), 2, 1);
            drawSocialMedia(SocialMediaType.VK, Heads.Vkontakte.getItem(), 1, 2);
            drawSocialMedia(SocialMediaType.YOUTUBE, Heads.Social_YouTube.getItem(), 1, 3);

            drawStatsViewer();
            drawFriendItem();

            drawMessageItem();
            drawSkinItem();

            remove(loadingIconSlot);
        });
    }

    private void drawSocialMedia(@NonNull SocialMediaType mediaType, @NonNull ItemStack itemStack, int x, int y) {
        Language language = getLanguage();

        ItemUtil.Builder itemBuilder = ItemUtil.getBuilder(itemStack);

        String socialMediaValue = targetProfile.getGamer().getSocialMediaLink(mediaType);

        itemBuilder.setName(translated("profile_player_media", "%media%", mediaType.getName(language)));
        itemBuilder.setLore(" ");

        if (socialMediaValue == null) {
            itemBuilder.addLore(translated("profile_player_media_unset", "%media%", mediaType.getName(language)));
        } else {
            itemBuilder.addLore(
                    translatedLore(
                            "profile_player_media_lore",
                            "%media%", mediaType.getName(language).toLowerCase(),
                            "%value%", socialMediaValue
                    )
            );
        }

        add(itemBuilder.build(), toSlot(x, y), clickEvent -> {
            if (socialMediaValue != null) {
                TextComponent component = new TextComponent(
                        language.getMessage(
                                "profile_player_media_message",
                                "%media%", mediaType.getName(language)
                        )
                );

                component.setHoverEvent(
                        new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{
                                new TextComponent(language.getMessage("profile_player_media_hover"))
                        }
                        )
                );

                component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, socialMediaValue));

                getPlayer().spigot().sendMessage(component);
                getPlayer().closeInventory();
            }
        });
    }

    private void drawPlayerSkull() {
        Gamer gamer = targetProfile.getGamer();

        int level = gamer.getLevel();

        Language language = getLanguage();

        ItemUtil.Builder skullBuilder = ItemUtil.getBuilder(targetProfile.getHeadSkull());

        String displayName;

        if (targetProfile.isNicked()) {
            displayName = gamer.getRealPrefixText() + gamer.getName();
        } else {
            displayName = gamer.getDisplayName();
        }

        skullBuilder.setName(
                translated("profile_player_main_title", "%name%", displayName)
        );
        skullBuilder.addLore(
                translatedLore(
                        "profile_player_main_lore",
                        "%player_id%", gamer.getPlayerId(),
                        "%level%", level,
                        "%status%", Leveling.getStatus(language, level)
                )
        );

        if (targetProfile.isHeadVisibleOnline()) {
            skullBuilder.addLore(
                    translatedLore(
                            "profile_player_main_online",
                            "%server_name%", targetProfile.getServerName()
                    )
            );
        } else {
            skullBuilder.addLore(
                    translatedLore(
                            "profile_player_main_offline",
                            "%time%", targetProfile.formatLastOnline(language)
                    )
            );
        }

        add(skullBuilder.build(), toSlot(6, 1));
    }

    private void drawStatsViewer() {
        Gamer gamer = getGamer();

        ItemUtil.Builder statsViewerBuilder = ItemUtil.getBuilder(Material.PAPER);
        statsViewerBuilder.setName(translated("profile_player_stats_name"));
        statsViewerBuilder.setLore(translatedLore("profile_player_stats_lore"));

        boolean hasUltra = gamer.getRanks().hasPermission("stats.view");

        if (!hasUltra) {
            statsViewerBuilder.addLore(translatedLore("profile_player_stats_lore_unavailable"));
        } else {
            statsViewerBuilder.addLore(translatedLore("profile_player_stats_lore_available"));
        }

        add(statsViewerBuilder.build(), toSlot(5, 2), clickEvent -> {
            if (hasUltra) {
                DiBukkitPlatform.getFactory()
                        .getBeanService()
                        .withBean(
                                ProfileGuisFactory.class,
                                factory -> factory.openStatsGui(gamer, targetProfile.getGamer(), null)
                        );
            }
        });
    }

    private void drawFriendItem() {
        Gamer gamer = getGamer();
        Gamer target = targetProfile.getGamer();

        boolean alreadyFriends = gamer.isFriend(target);

        ItemUtil.Builder friendBuilder = ItemUtil.getBuilder(Heads.Plush_F.getItem());
        friendBuilder.setName(
                translated(
                        "profile_player_" + (alreadyFriends ? "already" : "not") + "_friends_name"
                )
        );
        friendBuilder.setLore(
                translatedLore(
                        "profile_player_" + (alreadyFriends ? "already" : "not") + "_friends_lore"
                )
        );

        boolean canManageFriend = false;

        if (alreadyFriends) {
            canManageFriend = true;

            friendBuilder.addLore(translatedLore("profile_player_friends_remove_available"));
        } else if (!targetProfile.isVisibleOnline()) {
            friendBuilder.addLore(translatedLore("profile_player_friends_offline"));
        } else if (target.isSettingSet(Setting.FORBID_FRIEND_REQUESTS)) {
            friendBuilder.addLore(translatedLore("profile_player_friends_forbidden"));
        } else {
            canManageFriend = true;

            friendBuilder.addLore(translatedLore("profile_player_friends_add_available"));
        }

        // !!! JAVA 8 !!!
        boolean finalCanManageFriend = canManageFriend;

        add(friendBuilder.build(), toSlot(6, 2), clickEvent -> {
            if (!finalCanManageFriend) {
                return;
            }

            Connection.sendPacket(
                    new CPlayerCommand(
                            gamer.getPlayerId(),
                            "/friend " + (alreadyFriends ? "remove" : "add") + " " + target.getName()
                    )
            );

            getPlayer().closeInventory();
        });
    }

//    private void drawGiveItem() {
//        add(ItemUtil.getBuilder(Material.SKULL_ITEM)
//                .setDurability(3)
//                .setSkullValue(HeadTexture.GIFT_BLUE.getTexture())
//                .setName(translated("profile_gift_item_name"))
//                .setLore(translated("profile_gift_item_lore"))
//                .done(), toSlot(7, 3), (event) -> new GiftRankGui(player, targetProfile.getGamer()));
//    }

    private void drawDuelItem() {
        Gamer target = targetProfile.getGamer();


    }

    private void drawPartyItem() {
        Gamer gamer = getGamer();
        Gamer target = targetProfile.getGamer();

        ItemUtil.Builder partyBuilder = ItemUtil.getBuilder(Heads.Rainbow_P.getItem());
        partyBuilder.setName(translated("profile_player_party_name"));
        partyBuilder.setLore(translatedLore("profile_player_party_lore"));

        boolean canInvitePlayer = false;

        if (!targetProfile.hasParty()) {
            if (target.isSettingSet(Setting.FORBID_PARTY_REQUESTS) && !gamer.isFriend(target)) {
                partyBuilder.addLore(translatedLore("profile_player_party_forbidden"));
            } else if (!targetProfile.isVisibleOnline()) {
                partyBuilder.addLore(translatedLore("profile_player_party_offline"));
            } else {
                partyBuilder.addLore(translatedLore("profile_player_party_available"));

                canInvitePlayer = true;
            }
        } else {
            partyBuilder.addLore(translatedLore("profile_player_party_already_in"));

            if (targetProfile.isInParty(gamer.getPlayerId())) {
                partyBuilder.addLore(translatedLore("profile_player_party_already_in_yours"));
            } else {
                partyBuilder.addLore(translatedLore("profile_player_party_already_in_other"));
            }
        }

        // !!! JAVA 8 !!!
        boolean finalCanInvitePlayer = canInvitePlayer;

        add(partyBuilder.build(), toSlot(7, 2), clickEvent -> {
            if (!finalCanInvitePlayer) {
                return;
            }

            Connection.sendPacket(new CPlayerCommand(gamer.getPlayerId(), "/party " + target.getName()));

            getPlayer().closeInventory();
        });
    }

    private void drawMessageItem() {
        Gamer gamer = getGamer();
        Gamer target = targetProfile.getGamer();

        Language language = getLanguage();

        ItemUtil.Builder messageBuilder = ItemUtil.getBuilder(Material.BOOK_AND_QUILL);
        messageBuilder.setName(translated("profile_player_message_name"));
        messageBuilder.setLore(translatedLore("profile_player_message_lore"));

        //игнорируют настройку запрета только персонал и друзья игрока
        boolean canIgnoreSetting = gamer.isFriend(target) || gamer.getRanks().hasPermission("friend.request.bypass");
        boolean canMessagePlayer = false;

        if (!canIgnoreSetting && target.isSettingSet(Setting.FORBID_PRIVATE_MESSAGES)) {
            messageBuilder.addLore(translatedLore("profile_player_message_forbidden"));
        } else if (!targetProfile.isVisibleOnline()) {
            messageBuilder.addLore(translatedLore("profile_player_message_offline"));
        } else {
            messageBuilder.addLore(translatedLore("profile_player_message_available"));
            canMessagePlayer = true;
        }

        // !!! JAVA 8 !!!
        boolean finalCanMessagePlayer = canMessagePlayer;

        add(messageBuilder.build(), toSlot(5, 3), clickEvent -> {
            if (!finalCanMessagePlayer) {
                return;
            }

            String displayName = targetProfile.isNicked()
                    ? target.getRealPrefixText() + target.getName()
                    : target.getDisplayName();

            TextComponent component = new TextComponent(
                    language.getMessage(
                            "profile_player_message_message",
                            "%name%", displayName
                    )
            );

            component.setHoverEvent(
                    new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT, new BaseComponent[]{
                            new TextComponent(language.getMessage("profile_player_message_hover"))
                    }
                    )
            );

            component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + target.getName() + " "));

            getPlayer().spigot().sendMessage(component);
            getPlayer().closeInventory();
        });
    }

    private void drawSkinItem() {
        Gamer gamer = getGamer();

        String skinName = targetProfile.getHeadSkinName();

        ItemUtil.Builder skinBuilder = ItemUtil.getBuilder(Material.GOLD_LEGGINGS);
        skinBuilder.addItemFlags();
        skinBuilder.setName(translated("profile_player_setskin_name"));
        skinBuilder.setLore(translatedLore("profile_player_setskin_lore"));

        Skin gamerSkin = gamer.getSkin();
        String nameSkin = gamerSkin == null ? SkinsApi.DEFAULT_SKIN.getName() : gamerSkin.getName();
        boolean skinAlreadySelected = nameSkin.equalsIgnoreCase(skinName);

        if (skinAlreadySelected) {
            skinBuilder.addLore(translatedLore("profile_player_setskin_unavailable_already"));
        } else {
            skinBuilder.addLore(translatedLore("profile_player_setskin_available", "%skin%", skinName));
        }

        add(skinBuilder.build(), toSlot(6, 3), clickEvent -> {
            if (skinAlreadySelected) {
                return;
            }

            Player player = getPlayer();

            SkinUtil.selectSkinOnBungee(player, skinName);
            player.closeInventory();
        });
    }

    @Getter
    private class PlayerProfileView {

        private final Gamer gamer;

        /**
         * Текущий сервер просматриваемого игрока
         * Если он оффлайн, значение будет null
         */
        private String serverName;

        /**
         * Id участников Party этого игрока
         */
        private int[] partyMembers;

        private Skin headSkin;

        private PlayerProfileView(@NonNull Gamer gamer) {
            this.gamer = gamer;
        }

        // уже проверили, что игрок разрешил смотреть свой профиль,
        // теперь можно загружать всю остальную стату об игроке
        void loadPlayerInfo() {
            // подгружаем всю инфу об игроке заново только если
            // тот сейчас онлайн на сервере (т.е мог менять ее)
            // и онлайн НЕ на этом сервере
            gamer.loadPlayerId();
            gamer.loadProperties();
            gamer.loadRanks();
            gamer.loadFakeName();
            gamer.loadPrefixAndUpdateName();
            gamer.loadSkull();
            loadHeadSkin();
            gamer.loadSocialMediaLinks();
            gamer.loadAuthData();
            gamer.loadLastLoginTime();
        }

        // загружать имя сервера нужно после загрузки геймера из БД (желательно)
        CompletableFuture<?> loadNetworkInfo() {
            if (gamer.isOnline()) {
                serverName = gamer.getServer();

                Party party = gamer.getParty();

                if (party != null) {
                    partyMembers = new int[party.getMemberCount()];

                    int i = 0;

                    for (Gamer member : party.getMembers()) {
                        partyMembers[i++] = member.getPlayerId();
                    }
                }

                return CompletableFuture.completedFuture(null);
            }

            CNetworkPlayerLoadRequest request = new CNetworkPlayerLoadRequest(gamer.getPlayerId(), NetworkPlayerLoad.ALL_INFO);

            return Connection.awaitPacket(request).whenComplete((response, cause) -> {
                NetworkPlayerLoad.PlayerHolder player = response.getPlayers().get(0);

                serverName = player.getServer();

                if (player.getParty() != null) {
                    partyMembers = player.getParty().getMembers();
                }
            });
        }

        boolean hasParty() {
            return partyMembers != null;
        }

        boolean isInParty(int playerId) {
            return ArrayUtils.contains(partyMembers, playerId);
        }

        boolean isOnline() {
            return serverName != null;
        }

        boolean isVisibleOnline() {
            if (!isOnline()) {
                return false;
            }

            Gamer viewer = getGamer();

            if (viewer != null && viewer.hasPermission("core.staff.see.vanished.profile")) {
                return true;
            }

            return !isNicked() && !isVanished();
        }

        boolean isHeadVisibleOnline() {
            return isOnline() && !isNicked() && !isVanished();
        }

        boolean isNicked() {
            return gamer.getFakeName() != null;
        }

        boolean isVanished() {
            return gamer.isVanished();
        }

        ItemStack getHeadSkull() {
            return headSkin != null ? ItemUtil.getSkull(headSkin) : gamer.getSkull();
        }

        String getHeadSkinName() {
            return headSkin == null ? SkinsApi.DEFAULT_SKIN.getName() : headSkin.getName();
        }

        void loadHeadSkin() {
            if (!isNicked()) {
                headSkin = gamer.getSkin();
                return;
            }

            try {
                headSkin = SkinsApi.getPlayerSkin(gamer.getPlayerId(), gamer.getName()).get(3, TimeUnit.SECONDS);
            } catch (ExecutionException | RuntimeException | InterruptedException | TimeoutException e) {
                headSkin = gamer.getSkin();
            }
        }

        String formatLastOnline(@NonNull Language language) {
            LocalDateTime lastLogin = gamer.getLastLoginTimeRaw();

            if (lastLogin == null) {
                return language.getMessage("last_login_time_unknown");
            }

            return Duration.durationWordsMs(language,
                    java.time.Duration.between(lastLogin, LocalDateTime.now()).toMillis());
        }

    }

}