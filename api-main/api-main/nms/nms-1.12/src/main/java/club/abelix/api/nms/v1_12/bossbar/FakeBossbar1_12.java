package club.abelix.api.nms.v1_12.bossbar;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.NmsConstant;
import club.abelix.api.nms.bossbar.FakeBossbar;
import club.abelix.api.nms.packet.AbstractVisibleObject;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class FakeBossbar1_12 extends AbstractVisibleObject implements FakeBossbar {

    private final BossBar bossBar;
    private final NmsApi api;

    @Getter
    private String title;

    @Getter
    private float value;

    @Getter
    private boolean publicStatus;

    @Getter
    private String barStyle, barColor;

    FakeBossbar1_12(NmsApi api, String title, float value) {
        this.api = api;
        this.title = title;
        this.value = value;

        this.bossBar = Bukkit.createBossBar(title, BarColor.PURPLE, BarStyle.SOLID);
        this.bossBar.setVisible(true);
        this.bossBar.setProgress(value);
    }

    @Override
    public synchronized void setPublicStatus(boolean b) {
        publicStatus = b;

        if (publicStatus && !NmsConstant.c_customBossbarSending) {
            Bukkit.getOnlinePlayers().forEach(this::addReceiver);
        }
    }

    @Override
    public synchronized boolean addReceivers(@NotNull Collection<? extends Player> players) {
        boolean add = false;

        for (Player player : players) {
            if (receivers.add(player)) {
                api.getBossbarApi().addBossbar(player, this);
                bossBar.addPlayer(player);

                add = true;
            }
        }

        return add;
    }

    @Override
    public synchronized boolean removeReceiver(@NotNull Player player) {
        return removeReceivers(Collections.singleton(player));
    }

    @Override
    public synchronized boolean removeReceivers(@NotNull Collection<? extends Player> players) {
        boolean remove = false;

        for (Player player : players) {
            if (receivers.remove(player)) {
                api.getBossbarApi().removeBossbar(player, this);
                bossBar.removePlayer(player);

                remove = true;
            }
        }

        return remove;
    }

    @Override
    public synchronized void remove() {
        removeReceivers(new ArrayList<>(receivers));

        bossBar.setVisible(false);
    }

    @Override
    public void setBarStyle(String style) {
        this.barStyle = style;
        this.bossBar.setStyle(BarStyle.valueOf(style.toUpperCase()));
    }

    @Override
    public void setBarColor(String color) {
        this.barColor = color;
        this.bossBar.setColor(BarColor.valueOf(color.toUpperCase()));
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
        this.bossBar.setTitle(title);
    }

    @Override
    public void updateBossbar(Player player) {
        // на 1.12 боссбар не двигается за игроком
    }

    @Override
    public void setValue(float v) {
        if (v > 1F || v < 0F) {
            throw new IllegalArgumentException("Value must be between 0 and 1");
        }

        this.value = v;
        this.bossBar.setProgress(v);
    }

    @Override
    public void setValue(float v, float max) {
        if (v > max || v < 0F) {
            throw new IllegalStateException("Value must be between 0 and " + max);
        }

        setValue(v / max);
    }


}
