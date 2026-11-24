package club.abelix.api.server.lobby.mysterybox;

import lombok.NonNull;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MysteryBoxStorage {

    private static final List<MysteryBox> MYSTERY_BOX_LIST = new ArrayList<>();

    public static MysteryBox getMysteryBox(@NonNull Location location) {
        for (MysteryBox mysteryBox : MYSTERY_BOX_LIST) {
            if (mysteryBox.getLocation().equals(location)) {
                return mysteryBox;
            }
        }

        return null;
    }

    public static MysteryBox registerMysteryBox(@NonNull Location location) {
        MysteryBox mysteryBox = new MysteryBox(location.getBlock().getLocation());
        mysteryBox.create();
        registerMysteryBox(mysteryBox);

        return mysteryBox;
    }

    public static void registerMysteryBox(@NonNull MysteryBox mysteryBox) {
        MYSTERY_BOX_LIST.add(mysteryBox);
    }

    public static List<MysteryBox> getMysteryBoxes() {
        return Collections.unmodifiableList(MYSTERY_BOX_LIST);
    }
}
