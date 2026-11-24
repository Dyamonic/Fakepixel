package club.abelix.api.base.gui;

import java.util.Collection;
import java.util.Iterator;

public abstract class ScrollingZone<T> extends PagedZone<T> {

    // Отвечает за отрисовку зоны гуи, в которой отображается что-то постранично
    // При этом данная реализация подразумевает собой пролистывание по 1 предмету (скроллинг)

    protected ScrollingZone(Gui parent) {
        super(parent);
    }

    @Override
    public void draw() {
        Collection<T> items = getItems();

        int pageSize = this.slots.size();

        //по дефолту поставим кол-во страниц 1, ниже мы еще раз проверим
        pages = 1;

        //количество предметов, превышающих максимальное на странице
        //если их будет больше или равен 1, pages изменится
        int countMore = items.size() - pageSize;

        if (countMore > 0) {
            pages = countMore + 1;
        }

        if (page >= pages) {
            page = pages - 1;
        }

        if (page < 0) {
            page = 0;
        }

        Iterator<T> itr = skip(items, page);

        for (Integer slot : this.slots) {
            if (itr.hasNext()) {
                drawItem(itr.next(), slot);
            } else {
                parent.add(null, slot);
            }
        }
    }

    @Override
    public void addSlotRect(int sx, int sy, int w, int h) {
        if (h > 1) {
            throw new IllegalArgumentException("You can not specify more then 1 slot by Y in scrollable gui");
        }

        super.addSlotRect(sx, sy, w, h);
    }

    public void clear() {
        slots.clear();
    }

}
