package club.abelix.api.nms.entity.animation;

import club.abelix.api.nms.NmsApi;
import club.abelix.commons.version.GameVersion;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

//вот это анимации 1.12
//в идеале бы сюда добавить из 1.8 нужные названия
//сделать поля id и version
//например, SWING_HAND (id 0, version 47)
//и метод isSupported(NmsApi nmsApi), где nmsApi.getProtocolNumber() >= version
@RequiredArgsConstructor
@Getter
public enum FakeEntityAnimation {

    SWING_MAIN_HAND(0, GameVersion.MINECRAFT_1_12_2.getVersion(), -1),
    TAKE_DAMAGE(1, GameVersion.MINECRAFT_1_8.getVersion(), -1),
    LEAVE_BED(2, GameVersion.MINECRAFT_1_8.getVersion(), -1),
    SWING_OFF_HAND(3, GameVersion.MINECRAFT_1_12_2.getVersion(), -1),
    EAT_FOOD(3, GameVersion.MINECRAFT_1_8.getVersion(), GameVersion.MINECRAFT_1_8.getVersion()),
    CRITICAL_EFFECT(4, GameVersion.MINECRAFT_1_8.getVersion(), -1),
    MAGIC_CRITICAL_EFFECT(5, GameVersion.MINECRAFT_1_8.getVersion(), -1);

    private final int animationId, minimumProtocolVersion, maximumProtocolVersion;

    public boolean isSupported(@NonNull NmsApi nmsApi) {
        int protocolNumber = nmsApi.getProtocolNumber();

        return protocolNumber >= minimumProtocolVersion
                && (maximumProtocolVersion == -1 || protocolNumber <= maximumProtocolVersion);
    }

}
