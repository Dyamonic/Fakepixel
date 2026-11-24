package club.abelix.api.server.lobby.reward;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.reward.Reward;
import club.abelix.api.base.reward.impl.CoinReward;
import club.abelix.api.base.reward.impl.ExperienceReward;
import club.abelix.api.base.reward.impl.PlazmaReward;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.api.server.lobby.manager.LobbyManager;
import club.abelix.commons.data.dailyrewards.DailyRewardEntry;
import club.abelix.commons.data.dailyrewards.DailyRewardSqlHandler;
import club.abelix.commons.data.permission.Group;
import club.abelix.commons.language.Lang;
import club.abelix.commons.util.time.Duration;
import club.abelix.commons.util.time.TimeUtil;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.network.type.mode.Master;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.NonNull;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class DailyRewardsGui extends Gui {

    private static final int MAX_DAYS = 21;

    private static final Color[] FIREWORK_COLORS = new Color[]{
            Color.AQUA, Color.FUCHSIA, Color.ORANGE, Color.GREEN,
            Color.YELLOW, Color.RED, Color.SILVER
    };

    static {
        DAILY_REWARD_LIST = new TIntObjectHashMap<>();

        prepareRewardList();
    }

    //награды за каждый день для игроков
    private static final TIntObjectMap<List<Reward>> DAILY_REWARD_LIST;

    private static void prepareRewardList() {
        //награда игрокам за каждый день
        for (int day = 1; day <= MAX_DAYS; day++) {
            int experience = day * 100;
            int coins = day * 80;

            List<Reward> rewards = new ArrayList<>();

//            if (day % 7 == 0) {
//                experience += 1000;
//                coins += 500;
//            }
            if (day % 21 == 0){
                coins += 320;
                rewards.add(new PlazmaReward(25));
            }


            rewards.add(new ExperienceReward(experience));
//            rewards.add(new CoinReward(coins));
            rewards.add(new CoinReward(Master.BW,coins));
            rewards.add(new CoinReward(Master.DUELS, coins));

            DAILY_REWARD_LIST.put(day, rewards);
        }
    }

    static void open(@NonNull Gamer gamer) {
        HeartBeat.runAsync(() -> {
            DailyRewardEntry entry = DailyRewardSqlHandler.INSTANCE
                    .loadPlayerInfo(gamer.getPlayerId()).join();

            HeartBeat.runSync(() -> new DailyRewardsGui(gamer.getPlayer(), entry));
        });
    }

    private DailyRewardEntry entry;

    private DailyRewardsGui(@NonNull Player player,
                            @NonNull DailyRewardEntry entry) {

        super(player, Lang.of("daily_rewards_title"), 5);

        this.entry = entry;

        draw();
    }

    @Override
    public void draw() {
        int currentDay = (int) TimeUtil.getDayUTC();

        int dayStart = entry.getDayStart();
        int lastDay = entry.getLastDay();

        int x = 1;
        int y = 1;

        for (int i = 1; i <= MAX_DAYS; i++) {
            String skullValue = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjJlNzRiYjBkOWI1MmFkYmNjNzMyMDBkNDk0MGVhNWIwZWRjM2NhMmFhZDIwOTgwMzgzMGYxOWI0MTNlZTE2ZiJ9fX0=";

            List<Reward> dailyRewardList = DAILY_REWARD_LIST.get(i);

            List<String> iconLore = new ArrayList<>(translatedLore("daily_reward"));

            for (Reward reward : dailyRewardList) {
                iconLore.add(translated("reward_row", "%reward_name%", reward.getRewardName(getLanguage())));
            }

            iconLore.add(" ");

            int day = (dayStart == 0 ? currentDay : dayStart) + (i - 1);

            if (day > currentDay) {
                int delta = day - currentDay;

                iconLore.addAll(language.getMessageList("daily_reward_unavailable_not_collected"));

                if (day - 1 == currentDay) {
                    iconLore.addAll(language.getMessageList(
                            "daily_reward_unavailable_tomorrow"
                    ));
                } else {
                    iconLore.addAll(language.getMessageList(
                            "daily_reward_unavailable_days",
                            "%day_label%", Duration.durationWordsMs(language, TimeUnit.DAYS.toMillis(delta))
                    ));
                }
            } else if (lastDay == currentDay) {
                skullValue = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmJkMmY5MzQ3NmFiNjlmYWY1YTUxOWViNTgzMmRiODQxYzg1MjY2ZTAwMWRlNWIyNmU0MjdmNDFkOThlNWM3ZSJ9fX0=";

                iconLore.add(language.getMessage("daily_reward_unavailable_collected"));
            } else if (day < currentDay) {
                skullValue = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmJkMmY5MzQ3NmFiNjlmYWY1YTUxOWViNTgzMmRiODQxYzg1MjY2ZTAwMWRlNWIyNmU0MjdmNDFkOThlNWM3ZSJ9fX0=";

                iconLore.add(language.getMessage("daily_reward_unavailable_skip"));
            } else {
                skullValue = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTNiMjI4ZjcwYTM1ZDBhYTMyMzUwNDY3ZDllOGMwOWFhZTlhZTBhZTA4NzVmZGM4YzMxMWE4NzZiZTE5MDcxNyJ9fX0=";

                iconLore.add(language.getMessage("click_to_collect_reward"));
            }

            ItemStack iconItemStack = ItemUtil.getBuilder(Material.SKULL_ITEM)
                    .setDurability(3)
                    .setSkullValue(skullValue)
                    .setName(language.getMessage("daily_reward_name", "%day%", i))
                    .setLore(iconLore)
                    .build();

            // ниже - .!. .!. .!. JAVA 8 .!. .!. .!.
            int dayNumber = i;

            if (i == 8 || i == 15) {
                x = 1;
                y++;
            }

            add(iconItemStack, toSlot(x++, y), clickEvent -> {
                if (currentDay != day || lastDay >= currentDay) {
                    return;
                }

                dailyRewardList.forEach(reward -> reward.give(gamer));

                // этот день по счету = интервалу между обнулением статы
                if (dayNumber == MAX_DAYS) {
                    entry = entry.changeRewardDate(0, 0);

                    gamer.sendLocalizedMessage("daily_reward_thanks");

                    // донатеры получают 1 плазму
                    if (gamer.getRanks().hasPermission("group.donate")) {
                        gamer.addGold(1);
                        gamer.sendLocalizedMessage("daily_reward_cloud_plazma");
                    }

                    // закрываем инвентарь, все дни все таки...
                    player.closeInventory();
                } else {
                    entry = entry.changeRewardDate(dayStart == 0 ? currentDay : dayStart, currentDay);

                    gamer.sendLocalizedMessage("daily_reward_collected", "%num%", dayNumber);

                    // тут только перерисовываем заново
                    draw();
                }

                FalconAPI.getNmsApi().getSoundApi().playSound(getPlayer(), YSound.LEVEL_UP);

                // сохраняем данные в миске
                DailyRewardSqlHandler.INSTANCE.savePlayerInfo(entry);

                shootFireWork(player.getLocation());

                LobbyManager.INSTANCE.getLobby().getDailyRewardNotifyService().remove(gamer);
            });
        }
    }

    private void shootFireWork(@NonNull Location location) {
        Firework firework = location.getWorld().spawn(location, Firework.class);

        ThreadLocalRandom random = ThreadLocalRandom.current();

        Color color = FIREWORK_COLORS[random.nextInt(FIREWORK_COLORS.length)];

        FireworkMeta fireworkEffectMeta = firework.getFireworkMeta();
        fireworkEffectMeta.addEffect(FireworkEffect.builder()
                .withColor(color).withFade(color).withFlicker().with(FireworkEffect.Type.BALL)
                .build());

        firework.setFireworkMeta(fireworkEffectMeta);
    }

}
