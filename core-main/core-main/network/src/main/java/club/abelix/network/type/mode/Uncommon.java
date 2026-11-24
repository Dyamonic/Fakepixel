package club.abelix.network.type.mode;

import java.util.List;

import club.abelix.network.common.locale.UserLocale;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * MISC типы серверов, выживания, китпвп всякое
 * и такая дичь
 * <p>
 * У них префикс сервера = имени режима, поэтому дополнительных
 * полей тут не нужно. А по name() получать не можем, потому что
 * какой-нибудь скайблок должен иметь в префиксе не только первую букву
 * заглавную, а еще и 4-ую...
 */
@RequiredArgsConstructor
@Getter
public enum Uncommon implements ServerMode {
    SKYBLOCK("skyblock", "SkyBlock Hub"),
    SBI("sbi", "SkyBlock Island"),
    SBJ("sbj", "SkyBlock Jerry's Workshop"),
    SBD("sbd", "SkyBlock Dark Auction"),
    SBM("sbm", "SkyBlock Dwarven Mines"),
    SBDUNG("sbdung", "SkyBlock Dungeons"),
    SBDH("sbdh", "SkyBlock Dungeon Hub"),
    SBSPIDERS("sbspiders", "SkyBlock Spider's Den"),
    SBCRIS("sbcris", "SkyBlock Crimson Isle"),
    SBPARK("sbpark", "SkyBlock Park"),
    SBEND("sbend", "Skyblock End"),
    SBFARMS("sbfarms", "SkyBlock Farms"),
    SBMINES("sbmines", "SkyBlock Mines"),
    SBCH("sbch", "Skyblock Hollows"),
    SKYBLOCK_P("skyblock_p", "SkyBlock Private"),


    /**
     * SkyBlock Alpha
     */
    SKYBLOCK_HUB_ALPHA("skyblock_hub_alpha", "SkyBlock"),
    SBI_ALPHA("sbi_alpha", "SkyBlockIslands"),
    SBJ_ALPHA("sbj_alpha", "SkyBlockJerry"),
    SBD_ALPHA("sbd_alpha", "SkyBlockDark"),
    SBM_ALPHA("sbm_alpha", "SkyBlockMines"),
    SBDUNG_ALPHA("sbdung_alpha", "SkyblockDungeons"),
    SBDH_ALPHA("sbdh_alpha", "SkyblockDungeonHub"),
    SBSPIDERS_ALPHA("sbspiders_alpha", "SkyblockSpiders"),
    SBPARK_ALPHA("sbpark_alpha", "SkyblockPark"),
    SBEND_ALPHA("sbend_alpha", "SkyblockEnd"),
    SBFARMS_ALPHA("sbfarms_alpha", "SkyblockFarms"),
    SBMINES_ALPHA("sbmines_alpha", "SkyblockMines"),
    SBCH_ALPHA("sbch_alpha", "SkyblockHollows"),
    SKYBLOCK_P_ALPHA("skyblock_p_alpha", "SkyP"),


    //SkyBlock sandbox
    SKYBLOCK_SANDBOX("skyblock_sandbox", "SkyBlock SandBox"),
    SBI_SANDBOX("sbi_sandbox", "SandBox Islands"),
    SBJ_SANDBOX("sbj_sandbox", "SandBox Jerry"),
    SBD_SANDBOX("sbd_sandbox", "SandBox Dark Auction"),
    SBM_SANDBOX("sbm_sandbox", "SandBox Dwarven Mines"),
    SBCRIS_SANDBOX("sbcris_sandbox", "SandBox Crimson Isle"),
    SBDUNG_SANDBOX("sbdung_sandbox", "SandBox Dungeons"),
    SBDH_SANDBOX("sbdh_sandbox", "SandBox Dungeon Hub"),
    SBSPIDERS_SANDBOX("sbspiders_sandbox", "SandBox Spiders"),
    SBPARK_SANDBOX("sbpark_sandbox", "SandBox Park"),
    SBEND_SANDBOX("sbend_sandbox", "SandBox End"),
    SBFARMS_SANDBOX("sbfarms_sandbox", "SandBox Farms"),
    SBMINES_SANDBOX("sbmines_sandbox", "SandBox Mines"),
    SBCH_SANDBOX("sbch_sandbox", "SandBox Hollows"),
    SBG_SANDBOX("sbg_sandbox", "SandBox Garden"),
    SKYBLOCK_P_SANDBOX("skyblock_p_sandbox", "SkyP"),


    /**
     * SkyBlock Old (FOR YOUTUBE)!
     */
    O_SKYBLOCK("o_skyblock", "SkyBlock §6§lOLD"),
    O_SBI("o_sbi", "SkyBlockIslands"),
    O_SBJ("o_sbj", "SkyBlockJerry"),

    // Тренеровки бедварса
    BWPRACTICE("bwpractice", "BedWars Practice"),
    BUILDFFA("buildffa", "BuildFFA"),
    FASTBRIDGE("fastbridge", "Bridging"),
    MLF("mlf", "My Little Farm"),
    PIT("pit", "The Pit"),
    ;

    public static final Uncommon[] VALUES = values();

    private final String serverName;
    /**
     * Название режима
     */
    private final String displayName;

    Uncommon(String name) {
        this(name, name);
    }

    public static Uncommon getByName(@NonNull String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public List<String> getDescription(@NonNull UserLocale locale) {
        return locale.getMessageList("uncommon_" + name() + "_description");
    }

    @Override
    public int getTypeId() {
        return 3;
    }

    @Override
    public String getName() {
        return serverName.toUpperCase();
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public boolean isUncommon() {
        return true;
    }

}
