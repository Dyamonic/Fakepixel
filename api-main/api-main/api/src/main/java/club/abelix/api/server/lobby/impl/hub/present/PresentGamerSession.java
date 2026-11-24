package club.abelix.api.server.lobby.impl.hub.present;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamersession.GamerSession;
import club.abelix.api.base.gamersession.GamerSessionManager;
import club.abelix.api.server.lobby.impl.hub.present.repo.PlayerPresentsRepository;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public final class PresentGamerSession extends GamerSession {

    public static @NotNull PresentGamerSession get(final @NonNull Gamer gamer) {
        return (PresentGamerSession)
                GamerSessionManager.INSTANCE.getProvider(PresentGamerSession.class)
                        .getSession(gamer);
    }

    IntSet foundPresents;

    public PresentGamerSession(Gamer gamer) {
        super(gamer);
    }

    @Override
    public void load() {
        foundPresents = PlayerPresentsRepository.getFoundPresents(getGamer().getPlayerId());
    }

    public int getFoundPresentsCount() {
        return foundPresents.size();
    }

    public boolean isPresentFound(final int presentId) {
        return foundPresents.contains(presentId);
    }

    public boolean isPresentFound(final @NonNull Present present) {
        return isPresentFound(present.id());
    }

    public void addFoundPresent(final int presentId) {
        foundPresents.add(presentId);
        PlayerPresentsRepository.addFoundPresent(getGamer().getPlayerId(), presentId);
    }

    public void addFoundPresent(final @NonNull Present present) {
        addFoundPresent(present.id());
    }
}
