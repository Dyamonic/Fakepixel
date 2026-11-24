package club.abelix.games.fastbridge.commons.config;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.region.Region3Dim;
import club.abelix.games.fastbridge.commons.island.Island;
import club.abelix.games.fastbridge.commons.object.RelativeLocation;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

/**
 * @author Migel077
 */
@Log4j2
public record IslandConfig(
        String worldName,
        RelativeLocation relSpawnLocation,
        FakePlayerConfig settingsNpc,
        RelativeLocation relHologramLocation,
        Region3Dim relPlayableZone,
        Region3Dim relAllowedBuildingRelZone,
        RelativeLocation firstIslandCords,
        RelativeLocation islandsOffset,
        int islandsCount
) {

    public static IslandConfig create(@NonNull ConfigurationSection section) {
        return new IslandConfig(
                section.getString("WorldName"),
                RelativeLocation.fromString(section.getString("RelSpawnLocation")),
                FakePlayerConfig.create(section.getConfigurationSection("SettingsNpc")),
                RelativeLocation.fromString(section.getString("RelHologramLocation")),
                region3DimFromString(section.getString("RelPlayableZone")),
                region3DimFromString(section.getString("RelAllowedBuildingRelZone")),
                RelativeLocation.fromString(section.getString("FirstIslandCords")),
                RelativeLocation.fromString(section.getString("IslandsOffset")),
                section.getInt("IslandsCount")
        );

    }

    private static Region3Dim region3DimFromString(@NonNull String line) {
        try {
            String[] regionInfo = line.split(";");

            return new Region3Dim(
                    Integer.parseInt(regionInfo[0]),
                    Integer.parseInt(regionInfo[1]),
                    Integer.parseInt(regionInfo[2]),
                    Integer.parseInt(regionInfo[3]),
                    Integer.parseInt(regionInfo[4]),
                    Integer.parseInt(regionInfo[5]));
        } catch (Exception exception) {
            log.error("Failed parse Region3Dim: {}", line);
            throw new RuntimeException(exception);
        }
    }

    public Island createIsland(World world, int islandOffset) {
        return new Island(
                firstIslandCords.clone()
                        .add(islandsOffset.clone().multiply(islandOffset))
                        .toLocation(world),
                this,
                islandOffset
        );
    }

    public ItemStack getIcon(Gamer gamer) {
        ItemStack stack = new ItemStack(Material.STAINED_CLAY, getAmount(), getColor());
        ItemMeta meta = stack.getItemMeta();

        val language = gamer.getLanguage();

        meta.setDisplayName(language.getMessage("fast_bridge_world_" + worldName + "_name"));
        meta.setLore(language.getMessageList("fast_bridge_world_" + worldName + "_lore"));

        stack.setItemMeta(meta);
        return stack;
    }

    private byte getColor() {
        return switch (worldName) {
            case "ag_practice" -> 5; // лаймовый
            case "ag_practice_32" -> 13; // зелёный
            case "ag_practice_48" -> 4; // жёлтый
            case "ag_practice_64" -> 1; // оранжевый
            default -> 0;
        };
    }

    public byte getAmount() {
        return switch (worldName) {
            case "ag_practice" -> 16;
            case "ag_practice_32" -> 32;
            case "ag_practice_48" -> 48;
            case "ag_practice_64" -> 64;
            default -> 1;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IslandConfig that = (IslandConfig) o;
        return Objects.equals(worldName, that.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(worldName);
    }
}
