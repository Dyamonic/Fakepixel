package club.abelix.api.base.util.potion;

import club.abelix.api.base.actionitem.AbstractActionItem;
import club.abelix.api.base.actionitem.ActionItem;
import club.abelix.api.base.util.item.ItemUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PotionItem extends AbstractActionItem {

    public static final Cache<Player, PotionEffectType[]> POTION_USAGE_CACHE = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.SECONDS)
            .build();

    public static final PotionItem POTION_ITEM_SPEED = new PotionItem(
            "§bЗелье скорости II (1 мин.)",
            Arrays.asList(
                    "§7§oГоворят, сапоги-скороходы веками",
                    "§7§oпропитывались в этом растворе..",
                    "",
                    "§6§l⁎ §eВремя действия: 1 минута",
                    "",
                    "§d▸ Нажмите, чтобы использовать! ◂"
            ),
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWRjOWZmMTg0YWU3NjdkM2NiZmQ5YzNhYTJjN2U4OGIxMGY5YjU5MTI5N2ZmNjc2ZGE2MzlmYjQ0NDYyMzhjOCJ9fX0=",
            new PotionEffect(PotionEffectType.SPEED, 1200, 1)
    );

    public static final PotionItem POTION_ITEM_FIRE_RESISTANCE = new PotionItem(
            "§bЗелье огнестойкости I (3 мин.)",
            Arrays.asList(
                    "§7§oСамые опытные чародеи годами",
                    "§7§oпродумывали рецепт этого волшебного",
                    "§7§oпузырька, который дарует силы огня..",
                    "",
                    "§6§l⁎ §eВремя действия: 3 минуты",
                    "",
                    "§d▸ Нажмите, чтобы использовать! ◂"
            ),
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDY3ZGYzNWI3YTgwMjdkZGM4M2M5MTlkNzNmMGEzOTk4YWQ1NGViMGQ5NzNlZjcxNmFlNTA1YzY3NTU4MTBmNyJ9fX0=",
            new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 3600, 0)
    );

    public static final PotionItem POTION_ITEM_REGENERATION = new PotionItem(
            "§bЗелье регенерации II (15 сек.)",
            Arrays.asList(
                    "§7§oВы чувствуете прилив сил..",
                    "",
                    "§6§l⁎ §eВремя действия: 15 секунд",
                    "",
                    "§d▸ Нажмите, чтобы использовать! ◂"
            ),
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjNkMzU2Yjg2MTE4ZmY4YzQ4YzExOWVmM2QyYWM5MGMxYWQxMzkyODkzMDIyZjgwYjlkYzE2OGMzNmRiMmE1NiJ9fX0=",
            new PotionEffect(PotionEffectType.REGENERATION, 300, 1)
    );

    PotionEffect[] potionEffects;

    public PotionItem(@NonNull String title,
                      List<String> lore,
                      @NonNull String texture,
                      @NonNull PotionEffect... potionEffects) {

        super(ItemUtil.getBuilder(Material.SKULL_ITEM)
                .setDurability(3)

                .setName(title)
                .setLore(lore)
                .setSkullValue(texture)
                .setAmount(1)

                .build());

        this.potionEffects = potionEffects;
    }

    @Override
    public void handle(@NonNull ActionItem actionItem) {
        actionItem.setPlaceHandler(blockPlaceEvent -> blockPlaceEvent.setCancelled(true));

        actionItem.setInteractHandler(interactEvent -> {
            interactEvent.setCancelled(true);

            val player = interactEvent.getPlayer();
            val value = POTION_USAGE_CACHE.getIfPresent(player);
            val contain = new HashSet<>(Stream.of(potionEffects)
                    .map(PotionEffect::getType)
                    .collect(Collectors.toList())).containsAll(Arrays.asList(value == null ? new PotionEffectType[0] : value));

            if (value != null && contain)
                return;

            for (PotionEffect potionEffect : potionEffects) {

                if (player.hasPotionEffect(potionEffect.getType())) {
                    player.removePotionEffect(potionEffect.getType());
                }

                player.addPotionEffect(potionEffect);
            }

            player.playSound(player.getLocation(), Sound.DRINK, 1f, 0f);

            val item = interactEvent.getItem();
            item.setAmount(item.getAmount() - 1);

            player.setItemInHand(item);
            POTION_USAGE_CACHE.put(player, Arrays.stream(potionEffects).map(PotionEffect::getType).toArray(PotionEffectType[]::new));
        });
    }

}
