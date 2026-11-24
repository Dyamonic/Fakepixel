package club.abelix.api.base.gamer.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.language.Language;
import lombok.Getter;

@Getter
public class GamerLanguageChangeEvent extends GamerEvent {

    private final Language oldLanguage;
    private final Language language;

    public GamerLanguageChangeEvent(Gamer gamer, Language oldLanguage, Language language) {
        super(gamer);

        this.oldLanguage = oldLanguage;
        this.language = language;
    }
}
