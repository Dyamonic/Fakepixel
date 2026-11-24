package club.abelix.api.nms.entity.type.villager;

import club.abelix.api.nms.NmsApi;
import club.abelix.commons.version.GameVersion;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VillagerProfession {

    FARMER(GameVersion.MINECRAFT_1_8.getVersion()),
    LIBRARIAN(GameVersion.MINECRAFT_1_8.getVersion()),
    PRIEST(GameVersion.MINECRAFT_1_8.getVersion()),
    BACKSMITH(GameVersion.MINECRAFT_1_8.getVersion()),
    BUTCHER(GameVersion.MINECRAFT_1_8.getVersion()),
    NITWIT(GameVersion.MINECRAFT_1_12_2.getVersion());

    private final int minimumProtocolVersion;

    public boolean isSupported(@NonNull NmsApi nmsApi) {
        return nmsApi.getProtocolNumber() >= minimumProtocolVersion;
    }

}
