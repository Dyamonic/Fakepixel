package club.abelix.api.base.item.predicate;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.GameItem;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(staticName = "of")
public class PermissionPredicate implements GameItemPredicate {

    String permission;

    @Override
    public boolean match(Gamer gamer) {
        return gamer.hasPermission(permission) && !gamer.hasPermission(permission + ".disallowed");
    }

    @Override
    public List<String> getRequirementLore(GameItem item, Gamer gamer) {
        return List.of("you_dont_have_permission");
    }
}
