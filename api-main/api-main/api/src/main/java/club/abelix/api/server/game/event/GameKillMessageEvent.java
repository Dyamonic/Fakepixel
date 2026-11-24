package club.abelix.api.server.game.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.array.ObjectLinkedBuf;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Getter
public class GameKillMessageEvent extends GameEvent implements Cancellable {

    private final Gamer player;
    private final Gamer killer;
    private final List<KillMessageParameter> parameters = new ArrayList<>();
    private final boolean playerLeft;
    private final boolean by;
    @Setter
    private String localizationKey;
    @Setter
    private MessagePosition position = MessagePosition.CHAT;
    @Getter
    private EntityDamageEvent.DamageCause damageCause;

    @Getter
    @Setter
    private boolean cancelled;

    public GameKillMessageEvent(@NonNull Game game, @NonNull Gamer player, Gamer killer, EntityDamageEvent.DamageCause cause, @NonNull String localizationKey, boolean playerLeft, boolean by) {
        super(game);

        this.player = player;
        this.killer = killer;
        this.damageCause = cause;
        this.localizationKey = localizationKey;
        this.playerLeft = playerLeft;
        this.by = by;
    }

    public void setMessageParameter(@NonNull String key, @NonNull Function<Language, Object> objectSupplier) {
        parameters.add(new KillMessageParameter(key, objectSupplier));
    }

    public void removeMessageParameter(@NonNull String key) {
        parameters.removeIf(parameter -> parameter.key.equals(key));
    }

    public final void broadcast() {
        Function<Gamer, ObjectLinkedBuf> objectProvider = gamer -> {
            ObjectLinkedBuf array = new ObjectLinkedBuf();

            parameters.forEach(parameter ->
                    array.writer().writeNext(parameter.key, parameter.provider.apply(gamer.getLanguage())));

            return array;
        };

        switch (position) {
            case CHAT -> getGame().broadcastLocalized(localizationKey, objectProvider);
            case ACTION_BAR -> getGame().broadcastLocalizedActionbar(localizationKey, objectProvider);
        }
    }

    /**
     * Позиция, где будет отображаться сообщение
     */
    public enum MessagePosition {
        CHAT,
        ACTION_BAR
    }

    public static class KillMessageParameter {

        @Getter
        private final String key;
        private final Function<Language, Object> provider;

        public KillMessageParameter(String key, Function<Language, Object> provider) {
            this.key = key;
            this.provider = provider;
        }

        public Object apply(@NonNull Language language) {
            return provider.apply(language);
        }
    }

}
