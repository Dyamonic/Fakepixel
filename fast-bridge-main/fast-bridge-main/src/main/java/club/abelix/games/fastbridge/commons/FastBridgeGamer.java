package club.abelix.games.fastbridge.commons;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamersession.GamerSession;
import club.abelix.api.base.gamersession.GamerSessionManager;
import club.abelix.api.base.item.GameItemStorage;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.data.stat.StatsMode;
import club.abelix.games.fastbridge.commons.config.IslandConfig;
import club.abelix.games.fastbridge.commons.island.Island;
import club.abelix.games.fastbridge.commons.object.BuildingSession;
import club.abelix.games.fastbridge.cosmetic.api.category.BlocksCategory;
import club.abelix.games.fastbridge.cosmetic.api.category.FastBridgeCategory;
import club.abelix.games.fastbridge.cosmetic.item.block.BlocksCosmeticItem;
import club.abelix.games.fastbridge.game.FastBridgeGame;
import club.abelix.games.fastbridge.game.animation.AnimatedBlockRemover;
import club.abelix.games.fastbridge.game.inventory.item.ChangeWorldItem;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FastBridgeGamer extends GamerSession {

    private static final StatsMode STATS_MODE = FastBridgeGame.getStatsMode();

    final LinkedList<BuildingSession> endedSessions = new LinkedList<>();

    Island island;
    @Nullable
    BuildingSession currentBuildingSession;
    int[] blockSlots = {0, 1, 2};
    int toolSlot = 4;
    AtomicInteger blockPlaced = new AtomicInteger(0);
    AnimatedBlockRemover blockRemover;

    public FastBridgeGamer(Gamer gamer) {
        super(gamer);
    }

    public static FastBridgeGamer get(@NonNull final Gamer gamer) {
        return (FastBridgeGamer) GamerSessionManager.INSTANCE.getProvider(FastBridgeGamer.class).getSession(gamer);
    }

    public static FastBridgeGamer get(@NonNull final Player player) {
        return get(Gamer.getGamer(player));
    }

    @Override
    public void load() {
        blockRemover = FastBridgeGame.getInstance().getBlocksRemoverStorage().findDefault();
    }

    public int getLifetimeBestTime(final IslandConfig config) {
        val property = config.worldName();
        val time = getGamer().getLifetimeStat(STATS_MODE, property);
        return Math.max(time, 0);
    }

    public int getLifetimeBestTime() {
        if (island == null) throw new NullPointerException("Island is null");
        return getLifetimeBestTime(island.getRawConfig());
    }

    public void resetItems() {
        resetItems(null);
    }

    public void resetItems(@Nullable final BlocksCosmeticItem item) {
        val inventory = getGamer().getPlayer().getInventory();
        inventory.clear();

        val itemStorage = GameItemStorage.INSTANCE;

        val blocksCat = FastBridgeCategory.getCategory(BlocksCategory.class);
        BlocksCosmeticItem selectedBlock = item;

        if (selectedBlock == null) {
            selectedBlock = ((BlocksCosmeticItem) itemStorage.getSelected(blocksCat, getGamer()));
        }

        for (int slot : blockSlots) {
            inventory.setItem(slot, ItemUtil.getBuilder(selectedBlock.getItem()).setAmount(64).build());
        }

        inventory.setItem(toolSlot, ItemUtil.getBuilder(selectedBlock.chooseTool()).setUnbreakable().build());

        inventory.setItem(8, ChangeWorldItem.getItemStack());

        getGamer().getPlayer().updateInventory();
    }

    public void addSuccessfulAttempt(@NonNull final IslandConfig config, final int delta) {
        val property = config.worldName() + "_successful_attempt_count";
        getGamer().addStat(STATS_MODE, property, delta);
    }

    public void addSuccessfulAttempt(IslandConfig config) {
        addSuccessfulAttempt(config, 1);
    }

    public void addSuccessfulAttempt() {
        addSuccessfulAttempt(getIsland().getRawConfig(), 1);
    }

    public void addAttempt(@NonNull IslandConfig config, int delta) {
        val property = config.worldName() + "_attempt_count";
        getGamer().addStat(STATS_MODE, property, delta);
    }

    public void addAttempt(IslandConfig config) {
        addAttempt(config, 1);
    }

    public void addAttempt() {
        addAttempt(getIsland().getRawConfig(), 1);
    }

    public int getAttempts(@NonNull IslandConfig config) {
        val property = config.worldName() + "_attempt_count";
        return getGamer().getLifetimeStat(property);
    }

    public int getAttempts() {
        return getAttempts(getIsland().getRawConfig());
    }

    public int getSuccessfulAttempts(@NonNull IslandConfig config) {
        val property = config.worldName() + "_successful_attempt_count";
        return getGamer().getLifetimeStat(property);
    }

    public int getSuccessfulAttempts() {
        return getSuccessfulAttempts(getIsland().getRawConfig());
    }

    public boolean checkAndApplyLifetimeBestTime(int newTime, IslandConfig config) {
        val property = config.worldName();
        val time = getGamer().getLifetimeStat(property);
        if (time < newTime && time != 0) return false;
        if (time == 0) getGamer().addStat(STATS_MODE, property, newTime);
        else getGamer().takeStat(STATS_MODE, property, time - newTime);
        return true;
    }

    public boolean checkAndApplyLifetimeBestTime(int newTime) {
        if (island == null) throw new IllegalArgumentException("island is null");
        return checkAndApplyLifetimeBestTime(newTime, island.getRawConfig());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof FastBridgeGamer fbGamer)) return false;

        return getGamer().equals(fbGamer.getGamer());
    }

    @Override
    public int hashCode() {
        return getGamer().hashCode();
    }
}
