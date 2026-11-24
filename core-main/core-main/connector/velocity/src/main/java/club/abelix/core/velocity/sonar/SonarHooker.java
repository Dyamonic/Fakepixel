package club.abelix.core.velocity.sonar;

import club.abelix.core.velocity.StarshipVelocity;
import lombok.val;
import xyz.jonesdev.sonar.api.Sonar;

public final class SonarHooker {

    public void hook(StarshipVelocity core) {
        val listener = new SonarListener();

        core.getProxy().getEventManager().register(core, listener);
        Sonar.get().getEventManager().registerListener(listener);
    }
}
