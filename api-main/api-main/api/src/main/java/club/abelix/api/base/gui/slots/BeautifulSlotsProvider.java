package club.abelix.api.base.gui.slots;

public interface BeautifulSlotsProvider {

    /**
     *
     * @param itemsCount кол-во предметов, которое вы хотите поместить в эту GUI
     * @return кол-во строк, с которым должно быть создано GUI
     */
    int getRawCountForItems(int itemsCount);

    /**
     *
     * @param itemsCount кол-во предметов, которое вы хотите поместить в эту GUI
     * @param itemIndex index предмета, для которого нужно найти слот
     * @return слот для этого предмета в этом GUI
     */
    int getSlotForItem(int itemIndex, int itemsCount);

}
