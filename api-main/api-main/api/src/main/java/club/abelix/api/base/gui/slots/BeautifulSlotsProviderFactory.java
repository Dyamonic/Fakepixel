package club.abelix.api.base.gui.slots;

import club.abelix.api.base.gui.slots.impl.OneSlotBorderSlotsProvider;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BeautifulSlotsProviderFactory {

    public BeautifulSlotsProvider getInstance(BeautifulSlotsProviderType providerType) {
        return switch (providerType) {
            case ONE_SLOT_BORDER -> OneSlotBorderSlotsProvider.INSTANCE;
            default -> throw new IllegalArgumentException("No instances for '"+providerType+"'");
        };
    }


}