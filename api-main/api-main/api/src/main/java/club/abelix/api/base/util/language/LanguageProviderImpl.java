package club.abelix.api.base.util.language;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.nms.provider.LanguageProvider;
import club.abelix.commons.language.Language;
import lombok.NonNull;
import org.bukkit.entity.Player;

public class LanguageProviderImpl implements LanguageProvider {

    @Override
    public Language getLanguage(@NonNull Player player) {
        Gamer gamer = Gamer.getGamer(player);

        return gamer == null ? Language.defaultLanguage() : gamer.getLanguage();
    }
}
