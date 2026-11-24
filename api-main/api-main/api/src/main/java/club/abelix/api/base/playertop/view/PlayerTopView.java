package club.abelix.api.base.playertop.view;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.core.bukkit.HeartBeat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class PlayerTopView {

    @Getter
    protected final Gamer gamer;

    public abstract void create();

    public abstract void remove();

    public void update() {
        remove();
        HeartBeat.runSync(this::create);
    }

}
