package club.abelix.api.server.lobby.mode.npc;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.api.server.lobby.mode.selector.ModeSelectorGuisFactory;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.language.Lang;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import club.abelix.network.online.query.StatusQueryResult;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.Mini;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.network.type.mode.Uncommon;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ModeActionNpc implements ModeNpc {

    ServerMode mode;
    FakeEntityPlayer entity;
    Hologram hologram;
    ModeSelectorGuisFactory guisFactory;

    public ModeActionNpc(
            final @NotNull ServerMode mode
    ) {

        this.mode = mode;
        this.guisFactory = DiBukkitPlatform.getFactory()
                .getBeanService()
                .getBean(ModeSelectorGuisFactory.class)
                .orElseThrow();
    }

    @Override
    public void createEntities(
            final @NonNull Skin skin,
            final @NonNull Location location
    ) {
        entity = FalconAPI.getNmsApi()
                .getFakeEntityApi()
                .createPlayer(
                        location,
                        skin
                );

        entity.setPublicStatus(true);

        entity.setClickAction(player -> {
            val gamer = Gamer.getGamer(player);

            if (gamer == null) {
                return;
            }

            if (mode == Uncommon.SKYBLOCK) {
                gamer.connectMode(Uncommon.SKYBLOCK);
                return;
            }
            if (mode == Uncommon.SKYBLOCK_SANDBOX) {
                gamer.connectMode(Uncommon.SKYBLOCK_SANDBOX);
                return;
            }

            if (mode == Uncommon.BUILDFFA) {
                gamer.connectMode(Uncommon.BUILDFFA);
                return;
            }

            if (mode.isUncommon()) {
                guisFactory.openUncommonSelector(
                        gamer,
                        (Uncommon) mode,
                        null
                );
            } else {
                if (mode.isMaster()) {
                    gamer.connectMode(((Master) mode).getLobby());
                } else if (mode.isMini()) {
                    guisFactory.openModeActionGui(
                            (Mini) mode,
                            gamer,
                            Lang.of("single_mode_action_gui_title")
                                    .set("%mode%", ((Mini) mode).getMaster().getDisplayName())
                                    .compile(),
                            null
                    );
                } else {
                    gamer.connectMode(mode);
                }
            }
        });

        String modeName = mode.getDisplayName();

        this.hologram = new Hologram(location.clone().add(0, 1.6, 0));
        hologram.setPublicStatus(true);
        hologram.addTextLine(Lang.of(getModeConnectLocalizationKey()).compile());

        hologram.addTextLine(Lang.of("gamemode_npc_status_title").set("%mode%", modeName).compile());
        if (mode.isLobby()) {
            hologram.addTextLine(Lang.of("gamemode_npc_status_players")
                    .setRaw("%players%", "[word:PLAYER:0]").compile());
        } else {
            hologram.addTextLine(Lang.of("gamemode_npcgamelobby_status_players")
                    .setRaw("%players%", "[word:PLAYER:0]").compile());
        }

    }

    @Override
    public void addLabyEmote(
            final int animation
    ) {
        if (entity instanceof FakeEntityPlayer fakeEntityPlayer) {
            FalconAPI.getInstance()
                    .getLabyNpcEmotesService()
                    .addNPCEmotes(
                            fakeEntityPlayer,
                            animation
                    );
        }
    }

    @Override
    public void updateOnlineStatus(@NotNull StatusQueryResult result) {
        int totalViewers = entity.getViewers().size();

        if (totalViewers == 0) {
            return;
        }

        if (result.isEmpty()) {
            if (entity.getGlowingColor() != ChatColor.RED) {
                entity.setGlowingColor(ChatColor.RED);
            }

            entity.setGlowing(true);

            hologram.modifyTextLine(3, Lang.of("gamemode_npc_status_players")
                    .setRaw("%players%", 0).compile());
            hologram.modifyTextLine(1, Lang.of("gamemode_npc_status_"
                    + (mode.isGameLobby() ? "disabled" : "busy")).compile());
        } else {
            entity.setGlowing(true);
            entity.setGlowingColor(ChatColor.GREEN);

            hologram.modifyTextLine(1, Lang.of(getModeConnectLocalizationKey()).compile());

            int totalOnline = result.getTotalOnline();

            if (mode.isGameLobby()) {
                hologram.modifyTextLine(3, Lang.of("gamemode_npcgamelobby_status_players")
                        .setRaw("%players%", totalOnline).compile());
            } else {
                hologram.modifyTextLine(3, Lang.of("gamemode_npc_status_players")
                        .setRaw("%players%", totalOnline).compile());
            }

        }
    }

    private String getModeConnectLocalizationKey() {
        var langKeyConnect = "gamemode_npc_status_connect";

        if (mode == Uncommon.SKYBLOCK) {
            langKeyConnect = langKeyConnect.concat("_mode_skyblock");
        } else {
            langKeyConnect = langKeyConnect.concat("_mode_" + mode.getName().toLowerCase());
        }

        return langKeyConnect;
    }

}
