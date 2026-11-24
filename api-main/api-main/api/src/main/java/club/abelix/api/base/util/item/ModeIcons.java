package club.abelix.api.base.util.item;

import club.abelix.api.FalconAPI;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.Mini;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.network.type.mode.Uncommon;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;

import java.util.HashMap;
import java.util.Map;

@Deprecated
@UtilityClass
public class ModeIcons {
    private final Map<ServerMode, ItemStack> MODE_ICONS = new HashMap<>() {
        {
            // Masters
            //==================================
            put(Master.BW, ItemUtil.getBuilder(Material.BED).build());
            put(Master.DUELS, ItemUtil.getBuilder(Material.FISHING_ROD).build());
            put(Master.ARCADE, ItemUtil.getBuilder(Material.DIAMOND_BOOTS).build());
            //==================================
            //===Duels===
            // 1 - я строка
            put(Mini.DUEL_BOXING, new ItemStack(Material.RAW_FISH));
            put(Mini.DUEL_STICKFIGHT, new ItemStack(Material.STICK));
            put(Mini.DUEL_FIREBALL, new ItemStack(Material.FIREBALL));
            put(Mini.DUEL_FIREBALL_BATTLE, ItemUtil.getBuilder(Material.FIREBALL).glowing().build());
            put(Mini.DUEL_THEBRIDGE, new ItemStack(Material.STAINED_CLAY, 1, (short) 14));

            // 2 - я строка
            put(Mini.DUEL_NODEBUFF, FalconAPI.getNmsApi().getPlayerApi().getPotion(
                    PotionType.INSTANT_HEAL, true, false, false, 1
            ));
            put(Mini.DUEL_BUILDUHC, new ItemStack(Material.LAVA_BUCKET));
            put(Mini.DUEL_CLASSIC, new ItemStack(Material.FISHING_ROD));
            put(Mini.DUEL_SUMO, new ItemStack(Material.MILK_BUCKET));
            put(Mini.DUEL_GAPPLE, new ItemStack(Material.GOLDEN_APPLE, 1, (byte) 1));
            put(Mini.DUEL_BEDFIGHT,  new ItemStack(Material.BED));
            put(Mini.DUEL_BLOCKFIGHT, new  ItemStack(Material.WOOL, 1 , (byte) 11));

            //BedWars
            put(Mini.BEDWARS_SOLO, new ItemStack(Material.BED, 1));
            put(Mini.BEDWARS_RANKED, ItemUtil.getBuilder(Material.BED).glowing(true).build());
            put(Mini.BEDWARS_DOUBLES, new ItemStack(Material.BED, 2));
            put(Mini.BEDWARS_TRIO, new ItemStack(Material.BED, 3));
            put(Mini.BEDWARS_QUARDO, new ItemStack(Material.BED, 4));

            // BuildFFA
            put(Uncommon.BUILDFFA, new ItemStack(Material.GOLD_SWORD, 1));
            put(Uncommon.FASTBRIDGE, new ItemStack(Material.WOOL, 1, (short) 14));

            // Arcade
            put(Mini.BLOCK_PARTY, new ItemStack(Material.WOOL, 1));
            put(Mini.TNT_RUN, new ItemStack(Material.TNT, 1));
            put(Mini.TNT_TAG, ItemUtil.getBuilder(Material.GOLD_BOOTS).build());
            put(Mini.BOMB_LOBBERS, ItemUtil.getBuilder(Material.CHAINMAIL_HELMET).setColor(Color.RED).build());

            //SkyWars
            put(Mini.SKYWARS_SOLO,  new  ItemStack(Material.EYE_OF_ENDER,1));
            put(Mini.SKYWARS_TEAMS, new  ItemStack(Material.ENDER_CHEST, 1));
            put(Mini.SKYWARS_DUELS, new  ItemStack(Material.ENDER_PEARL, 1));
            put(Mini.SKYWARS_MINI,  new ItemStack(Material.ENDER_STONE, 1));
        }
    };

    public ItemStack get(@NonNull ServerMode mode) {
        return MODE_ICONS.computeIfAbsent(mode, m -> new ItemStack(Material.APPLE)).clone();
    }


    public boolean checkExist(@NonNull ServerMode mode) {
        return MODE_ICONS.get(mode) != null;
    }
}