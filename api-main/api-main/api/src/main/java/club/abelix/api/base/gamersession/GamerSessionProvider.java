package club.abelix.api.base.gamersession;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.network.common.logging.Logging;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: api</p>
 * <p>GamerSessionProvider creation on 17.04.2023 at 01:46</p>
 */

@Getter
@RequiredArgsConstructor
public class GamerSessionProvider<A extends GamerSession> {

    private final Class<A> sessionCls;
    private final HashMap<Gamer, A> loadedSessions = new HashMap<>();


    public A getSession(@NonNull Gamer gamer) {
        return loadedSessions.get(gamer);
    }

    public boolean load(@NonNull Gamer gamer) {
        A newSession;

        try {
            newSession = sessionCls.getConstructor(Gamer.class).newInstance(gamer);
            newSession.load();
        } catch (Exception e) {
            Logging.IMP.error("[Session | {}] Failed load session from gamer {}. Details:",
                    sessionCls.getName(), gamer.getName());
            e.printStackTrace();
            return false;
        }

        loadedSessions.put(gamer, newSession);
        Logging.IMP.info("[Session |{}] Success load session from gamer {}", sessionCls.getName(), gamer.getName());
        return true;
    }

    public void unload(@NonNull Gamer gamer) {
        A currentSession = getSession(gamer);

        if (currentSession == null) {
            return;
        }

        try {
            currentSession.unload();
        } catch (Exception any) {
            Logging.IMP.error("[Session | {}] Failed unload session from gamer {}. Details {}",
                    sessionCls.getName(), gamer.getName(), any.toString());
        } finally {
            loadedSessions.remove(gamer);
        }
    }

}
