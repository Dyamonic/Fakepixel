package club.abelix.api.nms.team;

import club.abelix.api.nms.NmsApi;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.plugin.Plugin;

import java.util.*;

@RequiredArgsConstructor
public abstract class FakeTeamApiBase implements FakeTeamApi {

    protected final NmsApi nmsApi;

    private final Map<Plugin, List<FakeTeam>> registeredTeamsMap = new HashMap<>();

    @Getter
    @Setter
    private DefaultFakeTeamProvider defaultFakeTeamProvider;

    @Override
    public FakeTeam createAndRegisterTeam(Plugin plugin, String name) {
        return createAndRegisterTeam(plugin, name, "", "");
    }

    @Override
    public FakeTeam createAndRegisterTeam(Plugin plugin, String name, String prefix, String suffix) {
        FakeTeam fakeTeam = createTeam(name, prefix, suffix);

        List<FakeTeam> fakeTeams = registeredTeamsMap.getOrDefault(plugin, new ArrayList<>());
        fakeTeams.add(fakeTeam);

        registeredTeamsMap.put(plugin, fakeTeams);

        return fakeTeam;
    }

    @Override
    public List<FakeTeam> getRegisteredTeams() {
        List<FakeTeam> fakeTeams = new ArrayList<>();

        for (List<FakeTeam> fakeTeamList : registeredTeamsMap.values()) {
            fakeTeams.addAll(fakeTeamList);
        }

        return fakeTeams;
    }

    @Override
    public List<FakeTeam> getRegisteredTeams(Plugin plugin) {
        return registeredTeamsMap.getOrDefault(plugin, new ArrayList<>());
    }

    @Override
    public void removeTeams(Plugin plugin) {
        List<FakeTeam> fakeTeams = registeredTeamsMap.remove(plugin);

        if (fakeTeams != null) {
            fakeTeams.forEach(FakeTeam::remove);
        }
    }

    @Override
    public void removeTeam(FakeTeam fakeTeam) {
        Plugin foundPlugin = null;

        for (Plugin plugin : registeredTeamsMap.keySet()) {
            Collection<FakeTeam> fakeTeams = getRegisteredTeams(plugin);

            if (fakeTeams.contains(fakeTeam)) {
                foundPlugin = plugin;
                break;
            }
        }

        if (foundPlugin != null) {
            List<FakeTeam> fakeTeams = getRegisteredTeams(foundPlugin);
            fakeTeams.remove(fakeTeam);

            registeredTeamsMap.put(foundPlugin, fakeTeams);
        }
    }

}
