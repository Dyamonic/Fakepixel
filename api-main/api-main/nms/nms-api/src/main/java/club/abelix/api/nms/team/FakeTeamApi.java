package club.abelix.api.nms.team;

import club.abelix.api.nms.team.listener.FakeTeamListener;
import lombok.NonNull;
import org.bukkit.plugin.Plugin;

import java.util.List;

public interface FakeTeamApi {

    default void registerTeamListener(@NonNull Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new FakeTeamListener(this), plugin);
    }

    FakeTeam createAndRegisterTeam(@NonNull Plugin plugin, @NonNull String name);

    FakeTeam createAndRegisterTeam(@NonNull Plugin plugin, @NonNull String name, @NonNull String prefix, @NonNull String suffix);

    FakeTeam createTeam(@NonNull String name);

    FakeTeam createTeam(@NonNull String name, @NonNull String prefix, @NonNull String suffix);

    List<FakeTeam> getRegisteredTeams();

    List<FakeTeam> getRegisteredTeams(@NonNull Plugin plugin);

    void removeTeams(@NonNull Plugin plugin);

    void removeTeam(@NonNull FakeTeam fakeTeam);

    void setDefaultFakeTeamProvider(@NonNull DefaultFakeTeamProvider fakeTeamProvider);

    DefaultFakeTeamProvider getDefaultFakeTeamProvider();

}
