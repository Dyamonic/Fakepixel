package club.abelix.duels;

import club.abelix.core.app.api.plugin.CorePlugin;
import club.abelix.duels.command.DuelsCommand;
import club.abelix.duels.mode.DuelMode;
import club.abelix.duels.processor.DuelsPacketProcessor;
import club.abelix.network.type.mode.Mini;

import java.util.HashMap;
import java.util.Map;

/**
 * @author artem
 */
public class DuelsCorePlugin extends CorePlugin {


    public static final Map<String, DuelMode> AVAILABLE_DUEL_MODES = new HashMap<>() {
        {
            put("classic", new DuelMode(Mini.DUEL_CLASSIC, 1));
            put("sumo", new DuelMode(Mini.DUEL_SUMO, 1));
            put("builduhc", new DuelMode(Mini.DUEL_BUILDUHC, 1));
            put("nodebuff", new DuelMode(Mini.DUEL_NODEBUFF, 1));
            put("stick", new DuelMode(Mini.DUEL_STICKFIGHT, 1));
            put("boxing", new DuelMode(Mini.DUEL_BOXING, 1));
            put("fireball", new DuelMode(Mini.DUEL_FIREBALL, 1));
            put("bridge", new DuelMode(Mini.DUEL_THEBRIDGE, 1));
            put("gapple", new DuelMode(Mini.DUEL_GAPPLE, 1));
            put("bedfight", new DuelMode(Mini.DUEL_BEDFIGHT, 1));
            put("blockfight", new DuelMode(Mini.DUEL_BLOCKFIGHT, 1));
        }
    };

    @Override
    public void onEnable() {
        getCore().getPluginManager().registerListener(this, new DuelsPacketProcessor());
        getCore().getCommandManager().addCommand(this, new DuelsCommand());
    }
}
