package club.abelix.network.type.mode;

import club.abelix.network.common.locale.UserLocale;
import lombok.Getter;
import lombok.NonNull;

import java.util.List;

@Getter
public enum Mini implements ServerMode {

    /**
     * BedWars
     */
    BEDWARS_SOLO("Solo", "§aSolo", Master.BW),
    BEDWARS_RANKED("§l§bRanked §l§4(Double)", "§l§bRanked §l§4(Double)", Master.BW),
    BEDWARS_DOUBLES("Doubles", "§cDoubles", Master.BW),
    BEDWARS_TRIO("3v3v3v3", "§a3v3v3v3", Master.BW),
    BEDWARS_QUARDO("4v4v4v4", "§a4v4v4v4", Master.BW),

    /**
     * Duels
     */
    // 1 - я строка
    DUEL_BOXING("Boxing", "Boxing", Master.DUELS),
    DUEL_STICKFIGHT("StickFight", "StickFight", Master.DUELS),
    DUEL_FIREBALL("FireballFight", "FireBall Fight", Master.DUELS),
    DUEL_FIREBALL_BATTLE("FireballBattle", "FireBall Battle", Master.DUELS),
    DUEL_THEBRIDGE("TheBridge", "The Bridge", Master.DUELS),

    // 2 - я Строка
    DUEL_NODEBUFF("NoDebuff", "NoDebuff", Master.DUELS),
    DUEL_BUILDUHC("UHC", "UHC", Master.DUELS),
    DUEL_CLASSIC("Classic", "Classic", Master.DUELS),
    DUEL_SUMO("Sumo", "Sumo", Master.DUELS),

    // 3 - я строка
    DUEL_GAPPLE("GApple", "GApple", Master.DUELS),
    DUEL_BEDFIGHT("BedFight", "Bed Fight", Master.DUELS),
//    DUEL_BATTLERUSH("BattleRush", "Battle Rush", Master.DUELS),
//    DUEL_QUAKE("Quake", "Quake", Master.DUELS),
//    DUEL_BUILDFIGHT("BuildFight", "§6§lBuild Fight", Master.DUELS),
//
//    // 2 - я Строка
//    DUEL_PARKOUR("Parkour", "Parkour", Master.DUELS),
//    DUEL_OP("OP", "OP", Master.DUELS),
//    DUEL_SD("SkyWars Duel", "SkyWars Duel", Master.DUELS),
    DUEL_BLOCKFIGHT("BlockFight", "BlockFight", Master.DUELS),

    // 3 - я строка
//    DUEL_BOW("Bow", "Bow Duel", Master.DUELS),
//    DUEL_SPLEEF("Spleef", "Spleef", Master.DUELS),
//    DUEL_TNT("TNT", "Bow Spleef", Master.DUELS),
//    DUEL_PEARLFIGHT("Pearl", "Pearl Fight", Master.DUELS),
//    DUEL_COMBO("Combo", "Combo duel", Master.DUELS),
    /**
     * Arcades
     */
    BLOCK_PARTY("BlockParty", "BlockParty", Master.ARCADE),
    TNT_RUN("TNTRun", "TNTRun", Master.ARCADE),
    BOMB_LOBBERS("Bombers", "Bombers", Master.ARCADE),
    TNT_TAG("TntTag", "TntTag", Master.ARCADE),
    DROPPER("Dropper", "Dropper", Master.ARCADE),
    SPEED_BUILDERS("SpeedBuilders", "SpeedBuilders", Master.ARCADE),
    MURDER_MYSTERY("MurderMystery", "MM", Master.MM),

    /**
     * LuckyWars
     */
    LUCKYWARS_SOLO("Solo", "§aSolo", Master.LW),

    /**
     * Exclusive Game
     */
    HALLOWEEN_SIMULATOR("Halloween Simulator", Master.EXCLUSIVE),

    /**
     * SkyWars
     */
    SKYWARS_DUELS("Duels", "§aSW Duels", Master.SW),
    SKYWARS_SOLO("Solo", "§aSolo", Master.SW),
    SKYWARS_TEAMS("Team", "§aTeam", Master.SW),
    SKYWARS_MINI("Mini", "§aMini", Master.SW)

    ;

    public static final Mini[] VALUES = values();

    private final int mask = 1 << ordinal();

    private final String displayName, shortName;

    private final Master master;

    Mini(String displayName, Master master) {
        this(displayName, displayName, master);
    }

    Mini(String displayName, String shortName, Master master) {
        this.displayName = displayName;
        this.shortName = shortName;

        this.master = master;
        this.master.addMini(this);

        if (isRating()) {
            master.setPrimaryRankedMini(this);
        }
    }

    public static Mini getByName(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public int getTypeId() {
        return 2;
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public String getServerName() {
        return getMaster().getServerName();
    }

    public List<String> getDescription(@NonNull UserLocale locale) {
        return locale.getMessageList("mini_" + name() + "_description");
    }

    @Override
    public boolean isMini() {
        return true;
    }

    public boolean isRating() {
        return this == Mini.BEDWARS_RANKED;
    }

    public boolean isSeasoning() {
        return false;
    }

    public int getMinLevel() {
        return 0;
    }
}
