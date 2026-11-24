package club.abelix.api.base.util.item;

import club.abelix.commons.data.permission.Group;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

@UtilityClass
public class RankIcons {

    private final Map<Group, ItemStack> RANK_ITEMS = new EnumMap<>(Group.class) {
        {
            put(Group.IRON,
                    ItemUtil.getBuilder(Material.LEATHER_CHESTPLATE)
                            .setColor(Color.fromRGB(127, 204, 25))
                            .build()
            );

            put(Group.GOLD,
                    ItemUtil.getBuilder(Material.LEATHER_CHESTPLATE)
                            .setColor(Color.fromRGB(222, 178, 35))
                            .build()
            );

            put(Group.DELUXE,
                    ItemUtil.getBuilder(Material.LEATHER_CHESTPLATE)
                            .setColor(Color.fromRGB(116, 156, 216))
                            .build()
            );

            put(Group.MASTER,
                    ItemUtil.getBuilder(Material.LEATHER_CHESTPLATE)
                            .setColor(Color.fromRGB(171, 82, 73))
                            .build()
            );

            put(Group.RUBIUM,
                    ItemUtil.getBuilder(Material.LEATHER_CHESTPLATE)
                            .setColor(Color.fromRGB(255, 0, 43))
                            .build()
            );

            put(Group.ULTRA,
                    ItemUtil.getSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODdmNWMzMGExMTdlZThhZTgwYzc4NThhYzQyYzY3YjJiYmU1ZTYwMGE2MGNlZWFhMjgxN2Y4NjJkZDg5MzMzZCJ9fX0=")
            );

            put(Group.SPONSOR,
                    ItemUtil.getSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmU2Y2EzY2U1ZDg5YWNiMDZhNjQzZjQ5ZjUyNzQ4NTVlZGVlNmVlYjFhNTQ1MTViMzA0MGExYjNmOGNiYjdhMSJ9fX0=")
            );
        }
    };

    public Collection<Group> getDonateGroups() {
        return RANK_ITEMS.keySet();
    }

    public ItemStack getRankIcon(@NonNull Group rank) {
        return RANK_ITEMS.computeIfAbsent(rank, m -> new ItemStack(Material.BARRIER))
                .clone();
    }

}
