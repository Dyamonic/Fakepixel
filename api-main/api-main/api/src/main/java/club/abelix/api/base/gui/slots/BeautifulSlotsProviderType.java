package club.abelix.api.base.gui.slots;

public enum BeautifulSlotsProviderType {

    ONE_SLOT_BORDER;

    public BeautifulSlotsProvider getInstance() {
        return BeautifulSlotsProviderFactory.getInstance(this);
    }

}
