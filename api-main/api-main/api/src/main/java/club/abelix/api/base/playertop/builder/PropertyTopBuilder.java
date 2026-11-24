package club.abelix.api.base.playertop.builder;

import club.abelix.api.base.playertop.provider.PropertyTopProvider;
import club.abelix.api.base.playertop.type.PlayerTop;
import lombok.NonNull;

public class PropertyTopBuilder extends SingleTopBuilder {

    private String property;
    private boolean ascending;

    public PropertyTopBuilder property(@NonNull String property) {
        this.property = property;
        return this;
    }

    public PropertyTopBuilder ascending() {
        this.ascending = true;
        return this;
    }

    @Override
    protected PlayerTop<?> generate() {
        provider(new PropertyTopProvider(property, maxPlayers, ascending));

        return super.generate();
    }
}
