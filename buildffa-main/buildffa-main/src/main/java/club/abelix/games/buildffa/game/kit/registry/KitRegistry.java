package club.abelix.games.buildffa.game.kit.registry;

import club.abelix.games.buildffa.game.kit.BaseKit;
import club.abelix.games.buildffa.game.kit.standard.*;
import com.google.common.collect.Sets;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Collection;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class KitRegistry {
    public static final KitRegistry REGISTRY = create(
            new BasicKitItem(),
            new ArcherKitItem(),
            new FishermanKitItem(),
            new AssassinKitItem(),
            new TankKitItem(),
            new PyroKitItem(),
            new CrazyKitItem(),
            new WitchKitItem(),
            new UHCKitItem(),
            new SnowKitItem(),
            new LumberjackKitItem()
    );

    Collection<BaseKit> kits;

    public static KitRegistry create(final BaseKit... kits) {
        return new KitRegistry(Sets.newHashSet(kits));
    }

    public void register(BaseKit kit) {
        kits.add(kit);
    }

    public BaseKit getKitById(int id) {
        return kits.stream()
                .filter(baseKit -> baseKit.getId() == id)
                .findFirst().orElse(null);
    }
}
