package club.abelix.api.server.game.manager;

import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.privategame.settings.PrivateGameSetting;
import club.abelix.api.server.game.privategame.settings.impl.PrivateGameOverrideSetting;
import club.abelix.api.server.game.setting.GameSetting;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class PrivateGameSettingsManager {
    Game game;

    @Getter
    Set<PrivateGameSetting> settings = new HashSet<>();


    public void addSetting(PrivateGameSetting setting) {
        settings.add(setting);
    }


    public void init() {
//        // ===Ломание блоков===
//        val blockBreakAllow = new PrivateGameOverrideSetting(
//                new ItemStack(Material.GRASS),
//                "block_break_can",
//                GameSetting.ALLOW_BLOCK_BREAK,
//                1
//
//        );
//        val blockBreakBreak = new PrivateGameOverrideSetting(
//                new ItemStack(Material.GRASS),
//                "block_break_deny",
//                GameSetting.ALLOW_BLOCK_BREAK,
//                2
//
//        );
//        val blockBreakSetting = new PrivateGameSetting(
//                new ItemStack(Material.GRASS),
//                "block_break",
//                Arrays.asList(blockBreakBreak, blockBreakAllow),
//                blockBreakAllow
//        );
//        addSetting(blockBreakSetting);

        // ===Максимальное колличество хп===
        val tenMaxHp = new PrivateGameOverrideSetting(
                new ItemStack(Material.POTION),
                "max_hp_5",
                GameSetting.PLAYER_HP,
                5

        );
        val stanMaxHp = new PrivateGameOverrideSetting(
                new ItemStack(Material.POTION),
                "max_hp_10",
                GameSetting.PLAYER_HP,
                20

        );
        val moreMaxHp = new PrivateGameOverrideSetting(
                new ItemStack(Material.POTION),
                "max_hp_normal",
                GameSetting.PLAYER_HP,
                20

        );
        val maxHp = new PrivateGameSetting(
                new ItemStack(Material.POTION),
                "max_hp",
                Arrays.asList(tenMaxHp, stanMaxHp, moreMaxHp),
                moreMaxHp
        );
        addSetting(maxHp);

        // ===Время респавна===
        val respawnFastTime = new PrivateGameOverrideSetting(
                new ItemStack(Material.IRON_SWORD),
                "respawn_fast_time",
                GameSetting.PLAYER_RESPAWN_TIME,
                1

        );
        val respawnNormalTime = new PrivateGameOverrideSetting(
                new ItemStack(Material.IRON_SWORD),
                "respawn_normal_time",
                GameSetting.PLAYER_RESPAWN_TIME,
                5

        );

        val respawnBigTime = new PrivateGameOverrideSetting(
                new ItemStack(Material.IRON_SWORD),
                "respawn_big_time",
                GameSetting.PLAYER_RESPAWN_TIME,
                15

        );
        val respawnTime = new PrivateGameSetting(
                new ItemStack(Material.IRON_SWORD),
                "respawn_time",
                Arrays.asList(respawnFastTime, respawnNormalTime, respawnBigTime),
                respawnNormalTime
        );
        addSetting(respawnTime);

        // ===Скорость ходьбы===
        val speedLower = new PrivateGameOverrideSetting(
                new ItemStack(Material.LEATHER_BOOTS),
                "speed_lower",
                GameSetting.PLAYER_SPEED,
                0.1f

        );
        val speedNormal = new PrivateGameOverrideSetting(
                new ItemStack(Material.IRON_BOOTS),
                "speed_normal",
                GameSetting.PLAYER_SPEED,
                0.2f

        );

        val speedFast = new PrivateGameOverrideSetting(
                new ItemStack(Material.DIAMOND_BOOTS),
                "speed_fast",
                GameSetting.PLAYER_SPEED,
                0.45f

        );
        val speed = new PrivateGameSetting(
                new ItemStack(Material.IRON_BOOTS),
                "speed",
                Arrays.asList(speedLower, speedNormal, speedFast),
                speedNormal
        );
        addSetting(speed);

    }
}
