package club.abelix.api.base.gui;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.sound.SoundType;
import club.abelix.commons.language.Lang;
import club.abelix.commons.util.math.MathUtil;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public abstract class PagedZone<T> {

    // Отвечает за отрисовку зоны гуи, в которой отображается что-то постранично
    // Вызывающее гуи должно вызвать методы draw() и drawButtons() друг за другом

    final Gui parent;

    final List<Integer> slots = new ArrayList<>();
    protected int page;
    int pages;

    protected PagedZone(@NonNull Gui parent) {
        this(parent, 0);
    }

    protected PagedZone(@NonNull Gui parent, int page) {
        this.parent = parent;
        this.page = page;
    }

    static <T> Iterator<T> skip(Collection<T> coll, int skip) {
        if (skip >= coll.size()) {
            return Collections.emptyIterator();
        }

        if (coll instanceof List && coll instanceof RandomAccess) {
            // можно быстро скипнуть элементы списка, создав sublist

            List<T> list = (List<T>) coll;

            return list.subList(skip, list.size()).iterator();
        }

        Iterator<T> itr = coll.iterator();

        for (int i = 0; i < skip && itr.hasNext(); i++) {
            itr.next();
        }

        return itr;
    }

    public void addSlot(int slot) {
        this.slots.add(slot);
    }

    public void addSlotRect(int sx, int sy, int w, int h) {
        for (int y = sy; y < sy + h; y++) {
            for (int x = sx; x < sx + w; x++) {
                addSlot(Gui.toSlot(x, y));
            }
        }
    }

    public void clearArea() {
        if (!(slots.isEmpty())) {
            slots.forEach(slot -> {
                parent.remove(slot);
            });
        }
    }

    public void draw() {
        Collection<T> items = getItems();

        int pageSize = this.slots.size();
        pages = MathUtil.ceil((double) items.size() / pageSize);

        if (page >= pages) {
            page = pages - 1;
        }

        if (page < 0) {
            page = 0;
        }

        int skip = page * pageSize;

        Iterator<T> itr = skip(items, skip);

        for (Integer slot : this.slots) {
            if (itr.hasNext()) {
                drawItem(itr.next(), slot);
            } else {
                parent.add(null, slot);
            }
        }
    }

    public void drawButtons() {
        int lastRow = parent.getSize() / 9 - 1;
        drawButtons(Gui.toSlot(2, lastRow), Gui.toSlot(6, lastRow));
    }

    public void drawButtons(int prevSlot, int nextSlot) {
        if (page > 0) {
            this.parent.add(getPrevPageIcon(), prevSlot, e -> {
                this.page--;
                this.parent.draw();

                FalconAPI.getNmsApi().getSoundApi().playSound(parent.getPlayer(), SoundType.CLICK);
            });
        } else {
            this.parent.add(null, prevSlot);
        }

        if (page < pages - 1) {
            this.parent.add(getNextPageIcon(), nextSlot, e -> {
                this.page++;
                this.parent.draw();

                FalconAPI.getNmsApi().getSoundApi().playSound(parent.getPlayer(), SoundType.CLICK);
            });
        } else {
            this.parent.add(null, nextSlot);
        }
    }

    public void drawButtons(int prevSlot, int nextSlot, short stainedGlassColor) {
        ItemStack stainedGlassIfArrowsIsNone = new ItemStack(Material.STAINED_GLASS_PANE);
        stainedGlassIfArrowsIsNone.setDurability(stainedGlassColor);

        if (page > 0) {
            this.parent.add(getPrevPageIcon(), prevSlot, e -> {
                this.page--;
                this.parent.draw();

                FalconAPI.getNmsApi().getSoundApi().playSound(parent.getPlayer(), SoundType.CLICK);
            });
        } else {
            this.parent.add(stainedGlassIfArrowsIsNone, prevSlot);
        }

        if (page < pages - 1) {
            this.parent.add(getNextPageIcon(), nextSlot, e -> {
                this.page++;
                this.parent.draw();

                FalconAPI.getNmsApi().getSoundApi().playSound(parent.getPlayer(), SoundType.CLICK);
            });
        } else {
            this.parent.add(stainedGlassIfArrowsIsNone, nextSlot);
        }
    }

    protected ItemStack getPrevPageIcon() {
        return ItemUtil.getSkullBuilder("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjg0ZjU5NzEzMWJiZTI1ZGMwNThhZjg4OGNiMjk4MzFmNzk1OTliYzY3Yzk1YzgwMjkyNWNlNGFmYmEzMzJmYyJ9fX0=")
                .setName(Lang.of("prev_page_name"))
                .setLore("lang:go_to_page:%page%:" + page + ";")
                .build();
    }

    protected ItemStack getNextPageIcon() {
        return ItemUtil.getSkullBuilder("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTYzMzlmZjJlNTM0MmJhMThiZGM0OGE5OWNjYTY1ZDEyM2NlNzgxZDg3ODI3MmY5ZDk2NGVhZDNiOGFkMzcwIn19fQ==")
                .setName(Lang.of("next_page_name"))
                .setLore("lang:go_to_page:%page%:" + (page + 2) + ";")
                .build();
    }

    protected abstract Collection<T> getItems();

    protected abstract void drawItem(T item, int slot);

}
