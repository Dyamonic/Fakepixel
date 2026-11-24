package club.abelix.api.base.util.hologram;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.hologram.factory.ArmorStandFactory;
import club.abelix.api.base.util.hologram.factory.ItemFactory;
import club.abelix.api.base.util.hologram.line.HologramLine;
import club.abelix.api.base.util.hologram.line.ItemHologramLine;
import club.abelix.api.base.util.hologram.line.TextHologramLine;
import club.abelix.api.nms.packet.AbstractVisibleObject;
import club.abelix.api.server.game.Game;
import club.abelix.commons.util.collection.CollectionUtil;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Getter
public class Hologram extends AbstractVisibleObject {

    /**
     * Строки голограммы (могут быть разного типа: предмет, текст, итд)
     */
    private final List<HologramLine> hologramLines = new ArrayList<>();

    /**
     * Получатели (те, кто видят голограмму)
     */
    private final Set<Player> receivers = CollectionUtil.createConcurrentWeakSet();

    private final ArmorStandFactory armorStandFactory;
    private final ItemFactory itemFactory;

    private int visibilityDistance = 30 * 20;

    /**
     * Статус публичности голограммы
     */
    private boolean publicStatus;

    private Location location;

    public Hologram(@NonNull Location location) {
        this(
                location,
                FalconAPI.getNmsApi().getFakeEntityApi()::createArmorStand,
                FalconAPI.getNmsApi().getFakeEntityApi()::createItem
        );
    }

    public Hologram(@NonNull Game game, @NonNull Location location) {
        this(
                location,
                loc -> game.createEntity(api -> api.createArmorStand(loc)),
                (loc, item) -> game.createEntity(api -> api.createItem(loc, item))
        );
    }

    public Hologram(@NonNull Location location,
                    @NonNull ArmorStandFactory armorStandFactory,
                    @NonNull ItemFactory itemFactory) {
        this.location = location;
        this.armorStandFactory = armorStandFactory;
        this.itemFactory = itemFactory;
    }

    private void addLine(int index, @NonNull HologramLine hologramLine) {
        hologramLine.setPublicStatus(publicStatus);

        hologramLines.add(index, hologramLine);

        for (Player player : receivers) {
            hologramLine.showTo(player);
        }
    }

    public void addTextLine(@NonNull String text) {
        addLine(
                hologramLines.size(),
                new TextHologramLine(armorStandFactory, calculateLineLocation(hologramLines.size()), text, this.visibilityDistance)
        );
    }

    public void addTextLine(@NonNull String text, @NonNull Boolean marker, @NonNull Boolean isSmall) {
        addLine(
                hologramLines.size(),
                new TextHologramLine(armorStandFactory, calculateLineLocation(hologramLines.size()), text, marker, isSmall, this.visibilityDistance)
        );
    }

    public void addItemLine(@NonNull ItemStack itemStack) {
        addLine(
                hologramLines.size(),
                new ItemHologramLine(
                        itemFactory, armorStandFactory,
                        calculateLineLocation(hologramLines.size()), itemStack,
                        this.visibilityDistance
                )
        );
    }

    public void insertTextLine(int index, @NonNull String text) {
        addLine(index, new TextHologramLine(armorStandFactory, calculateLineLocation(hologramLines.size()), text, this.visibilityDistance));
        teleport(location);
    }

    public void insertItemLine(int index, @NonNull ItemStack itemStack) {
        addLine(index, new ItemHologramLine(
                itemFactory, armorStandFactory,
                calculateLineLocation(hologramLines.size()), itemStack, this.visibilityDistance)
        );

        teleport(location);
    }

    public void modifyTextLine(int line, @NonNull String text) {
        HologramLine hologramLine = getHologramLine(line);

        if (hologramLine instanceof TextHologramLine) {
            ((TextHologramLine) hologramLine).setText(text);
        }
    }

    public void modifyItemLine(int line, @NonNull ItemStack itemStack) {
        HologramLine hologramLine = getHologramLine(line);

        if (hologramLine instanceof ItemHologramLine) {
            ((ItemHologramLine) hologramLine).setItemStack(itemStack);
        }
    }

    public void removeLine(int lineNumber) {
        HologramLine hologramLine = hologramLines.remove(lineNumber - 1);
        hologramLine.remove();

        teleport(location);
    }

    public HologramLine getHologramLine(int line) {
        Preconditions.checkArgument(hologramLines.size() >= line && line > 0, "Invalid line number: " + line);

        return hologramLines.get(line - 1);
    }

    public void teleport(Location location) {
        this.location = location;

        for (int i = 0; i < hologramLines.size(); i++) {
            HologramLine line = hologramLines.get(i);

            line.teleport(calculateLineLocation(i));
        }
    }

    public void setClickAction(Consumer<Player> clickAction) {
        hologramLines.forEach(hologramLine -> hologramLine.setClickAction(clickAction));
    }

    public void setPublicStatus(boolean publicStatus) {
        this.publicStatus = publicStatus;

        for (HologramLine hologramLine : hologramLines) {
            hologramLine.setPublicStatus(publicStatus);
        }
    }

    @Override
    public void remove() {
        clear();
    }

    @Deprecated
    public void clear() {
        hologramLines.forEach(HologramLine::remove);
        hologramLines.clear();
    }

    public void removeReceivers() {
        new ArrayList<>(receivers).forEach(this::removeReceiver);
    }

    @Override
    public synchronized boolean addReceivers(@NonNull @NotNull Collection<? extends Player> players) {
        boolean status = receivers.addAll(players);

        if (status) {
            players.forEach(player -> hologramLines.forEach(hologramLine -> hologramLine.showTo(player)));
        }

        return status;
    }

    @Override
    public synchronized boolean removeReceivers(@NonNull @NotNull Collection<? extends Player> players) {
        boolean status = receivers.removeAll(players);

        if (status) {
            players.forEach(player -> hologramLines.forEach(hologramLine -> hologramLine.remove(player)));
        }

        return status;
    }

    public void setVisibilityDistance(int visibilityDistance) {
        this.visibilityDistance = visibilityDistance;

        hologramLines.forEach(hologramLine -> hologramLine.setVisibilityDistance(visibilityDistance));
    }

    public void update(@NonNull Gamer gamer) {
        Player player = gamer.getPlayer();

        removeReceiver(player);
        addReceiver(player);
    }

    public Location calculateLineLocation(int index) {
        Location location = this.location.clone();

        double space = 0;

        for (int i = 0; i < index; i++) {
            space += hologramLines.get(i).getLineDistanceModifier();
        }

        return location.subtract(0, space, 0);
    }


}
