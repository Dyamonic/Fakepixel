package club.abelix.api.server.game.hotbar;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.hotbar_new.ActionType;
import club.abelix.api.base.hotbar_new.Hotbar;
import club.abelix.api.base.hotbar_new.HotbarClickAction;
import club.abelix.api.base.hotbar_new.HotbarService;
import club.abelix.api.base.hotbar_new.builder.HotbarBuilder;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.gui.SpectatorGui;
import club.abelix.api.server.game.manager.GameRedirectLobbyManager;
import club.abelix.api.server.game.privategame.factory.PrivateGameGuiFactory;
import club.abelix.api.server.game.privategame.gui.PrivateGameSettingsGui;
import club.abelix.di.api.annotation.Bean;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.type.connect.ConnectStatus;
import club.abelix.network.type.mode.Lobby;
import club.abelix.network.type.mode.Mini;
import lombok.val;
import org.bukkit.Material;

import java.util.EnumSet;

@Bean
public class GameHotbar {

    public static final String SPECTATOR_HOTBAR_ID = "spectator";
    public static final String END_HOTBAR_ID = "end";

    public static String formattedPrivateGamesLobbyHotbarId(Game game) {
        return "game_lobby/" + game.getServerGameId() + "/private";
    }

    private static final EnumSet<Mini> BEDWARS_MINIS = EnumSet.of(
            Mini.BEDWARS_DOUBLES,
            Mini.BEDWARS_SOLO,
            Mini.BEDWARS_TRIO,
            Mini.BEDWARS_QUARDO
    );

    public static String formattedWaitingLobbyHotbarId(Game game) {
        return "game_lobby/" + game.getServerGameId();
    }

    public void registerGame(Game game) {
        register(createWaitingHotbar(game));
        register(createPrivateGamesHotbar(game));
    }

    public void registerDefaults() {
        register(createSpectatorHotbar());
        register(createEndGameHotbar());
    }

    private void register(Hotbar hotbar) {
        val hotbarService = DiBukkitPlatform.getFactory()
                .getBeanService()
                .getBean(HotbarService.class)
                .orElseThrow();

        hotbarService.registerHotbar(hotbar);
    }

    private Hotbar createPrivateGamesHotbar(Game game) {
        val builder = createBase(formattedPrivateGamesLobbyHotbarId(game));

        if (!game.isPrivateGame()) {
            return builder.build();
        }


        boolean isBedwars = BEDWARS_MINIS.contains(game.getMini());

        builder.slot(0)
                .itemSupplier(gamer -> {
                    if (!isPrivateLeader(gamer)) {
                        return ItemUtil.getBuilder(Material.AIR).build();
                    }

                    val language = gamer.getLanguage();
                    return ItemUtil.getBuilder(Material.REDSTONE_COMPARATOR)
                            .setName(language.getMessage("game_lobby_private_settings_icon"))
                            .setLore(language.getMessageList("game_lobby_private_settings_icon_lore"))
                            .build();
                })
                .onAny(action -> {
                    if (!isPrivateLeader(action.gamer())) return;
                    openPrivateGameSettings(action);
                })
                .done();

        builder.slot(1)
                .itemSupplier(gamer -> {
                    if (!isPrivateLeader(gamer)) {
                        return ItemUtil.getBuilder(Material.AIR).build();
                    }

                    val language = gamer.getLanguage();
                    return ItemUtil.getBuilder(Material.NOTE_BLOCK)
                            .setName(language.getMessage("game_lobby_private_start_icon"))
                            .setLore(language.getMessageList("game_lobby_private_start_icon_lore"))
                            .build();
                })
                .onAny(action -> {
                    if (!isPrivateLeader(action.gamer())) return;
                    startPrivateGame(action);
                })
                .done();

        if (isBedwars) {
            builder.slot(4)
                    .itemSupplier(gamer -> {
                        val language = gamer.getLanguage();
                        return ItemUtil.getBuilder(Material.EMERALD)
                                .setName(language.getMessage("game_lobby_private_team_selector_icon"))
                                .setLore(language.getMessageList("game_lobby_start_team_selector_lore"))
                                .build();
                    })
                    .onAny(this::openPrivateGameTeamSelector)
                    .done();
        }

        return builder.build();
    }


    private Hotbar createEndGameHotbar() {
        return createBase(END_HOTBAR_ID)
                .slot(4)
                .itemSupplier(g -> {
                    val language = g.getLanguage();

                    return ItemUtil.getBuilder(Material.PAPER)
                            .setName(language.getMessage("game_spectator_replay_icon"))
                            .setLore(language.getMessageList("game_spectator_replay_icon_lore"))
                            .build();
                })
                .on(ActionType.RIGHT_CLICK, this::replay)
                .done()
                .build();
    }

    private Hotbar createWaitingHotbar(Game game) {
        return createBase(formattedWaitingLobbyHotbarId(game)).build();
    }

    private Hotbar createSpectatorHotbar() {
        return createBase(SPECTATOR_HOTBAR_ID)
                .slot(0)
                .itemSupplier(g -> {
                    val language = g.getLanguage();

                    return ItemUtil.getBuilder(Material.COMPASS)
                            .setName(language.getMessage("game_spectator_compass_icon"))
                            .setLore(language.getMessageList("game_spectator_compass_icon_lore"))
                            .build();
                })
                .onAny(action -> new SpectatorGui(action.gamer().getGame(), action.gamer()))
                .done()
                .slot(4)
                .itemSupplier(g -> {
                    val language = g.getLanguage();

                    return ItemUtil.getBuilder(Material.PAPER)
                            .setName(language.getMessage("game_spectator_replay_icon"))
                            .setLore(language.getMessageList("game_spectator_replay_icon_lore"))
                            .build();
                })
                .on(ActionType.RIGHT_CLICK, this::replay)
                .done()
                .build();
    }

    private HotbarBuilder createBase(String id) {
        return HotbarBuilder.of(id)
                .slot(8)
                .itemSupplier(g -> {
                    val language = g.getLanguage();

                    return ItemUtil.getBuilder(Material.BED)
                            .setName(language.getMessage("game_lobby_icon"))
                            .setLore(language.getMessageList("game_lobby_icon_lore"))
                            .build();
                })
                .on(ActionType.RIGHT_CLICK, this::redirectToLobby)
                .done();
    }

    private void openPrivateGameSettings(HotbarClickAction action) {
        val gamer = action.gamer();
        val game = gamer.getGame();

        if (game == null || !game.isPrivateGame()) {
            return;
        }

        DiBukkitPlatform.getFactory().getBeanService().withBeanOrElse(
                PrivateGameGuiFactory.class,
                factory -> factory.openSettings(gamer, game),
                () -> {
                    new PrivateGameSettingsGui(gamer, game);
                }
        );
    }

    private void openPrivateGameTeamSelector(HotbarClickAction action) {
        val gamer = action.gamer();
        val game = gamer.getGame();

        if (game == null || !game.isPrivateGame()) {
            return;
        }

        DiBukkitPlatform.getFactory().getBeanService().withBeanOrElse(
                PrivateGameGuiFactory.class,
                factory -> factory.openTeamSelector(gamer, game),
                () -> {
                    val language = gamer.getLanguage();
                    gamer.sendMessage(language.getMessage("private_game_unsupported_team_selector_mode"));
                }
        );
    }

    private void replay(HotbarClickAction action) {
        val gamer = action.gamer();
        val game = gamer.getGame();

        if (game == null) {
            return;
        }

        gamer.connectMode(game.getMini());
    }

    private void startPrivateGame(HotbarClickAction action) {
        val gamer = action.gamer();
        val game = gamer.getGame();

        if (!game.isPrivateGame()) {
            gamer.sendLocalizedMessage("internal_error");
            return;
        }

        gamer.playSound(YSound.VILLAGER_YES);
        game.getWaitingManager().setStartTime(5);
        game.getWaitingManager().startTimer();
    }

    private void redirectToLobby(HotbarClickAction action) {
        val gamer = action.gamer();
        val manager = GameRedirectLobbyManager.INSTANCE;

        if (manager.isWaitSend(gamer)) {
            manager.remove(gamer);
            return;
        }

        manager.add(gamer, () -> {
            if (gamer.getGame() == null) {
                return;
            }

            gamer.connectMode(true, CoreUtil.getMaster().getLobby())
                    .whenComplete((connectStatus, throwable) -> {
                        if (throwable != null) {
                            Logging.IMP.stacktrace(throwable, "Failed send gamer {} to lobby", gamer.getName());
                        }

                        if (connectStatus == ConnectStatus.SUCCESS) {
                            return;
                        }
                        gamer.connectMode(Lobby.HUB);
                    });
        }, 3);
    }

    private boolean isPrivateLeader(Gamer gamer) {
        val party = gamer.getParty();

        return party != null && party.isLeader(gamer);
    }
}
