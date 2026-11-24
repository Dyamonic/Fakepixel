package club.abelix.api.server.lobby.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.data.setting.media.SocialMediaSqlHandler;
import club.abelix.commons.data.setting.media.SocialMediaType;
import club.abelix.commons.language.Language;
import club.abelix.core.bukkit.event.YEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

//вызывается, когда игрок захотел изменить одну из ссылок на соц. сеть

@RequiredArgsConstructor
@Getter
public class SocialMediaLinkChangeEvent extends YEvent {

    private final Gamer gamer;
    private final SocialMediaType mediaType;

    //принимает как аргумент
    private final Function<String, String> function = new Function<String, String>() {

        @Override
        public String apply(String chatMessage) {
            String value = mediaType.parse(chatMessage);

            Language language = gamer.getLanguage();

            if (value == null) {
                gamer.sendLocalizedMessage("media_bad_format");
                return null;
            }

            if (value.equals(gamer.getSocialMediaLink(mediaType))) {
                gamer.sendLocalizedMessage("media_already_set",
                        "%media%", mediaType.getName(language));
                return value;
            }

            //ищем игроков с такой же ссылкой, повторять нельзя, по краней мере если это ДС или еще что-то
            SocialMediaSqlHandler.INSTANCE.findSocialMediaLink(mediaType, value, result -> {
                if (result) {
                    gamer.sendLocalizedMessage("media_already_set_by_another_player",
                            "%media%", mediaType.getName(language));
                    return;
                }

                gamer.sendLocalizedMessage("media_set",
                        "%usage%", mediaType.getUsage(language), "%value%", value);

                gamer.changeSocialMediaLink(mediaType, value);
            });

            return value;
        }
    };

}
