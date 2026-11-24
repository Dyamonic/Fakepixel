package club.abelix.core.velocity.alpha;

import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.alpha.command.AlphaCommand;
import club.abelix.core.velocity.config.Configuration;
import club.abelix.network.common.logging.Logging;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>AlphaServerWhitelist creation on 20.08.2023 at 22:34</p>
 */

@Getter
@RequiredArgsConstructor
public class AlphaServerWhitelist {

    private final StarshipVelocity starship;
    private final List<String> availableNickname = new ArrayList<>();
    private Boolean enabled;


    public void load() {
        Configuration configuration = starship.loadConfiguration("alpha_whitelist.yml");

        if (configuration == null) {
            Logging.IMP.error("[Alpha] Failed load alpha_whitelist.yml");
            return;
        }

        starship.getCommandManager().addCommand(new AlphaCommand());

        availableNickname.clear();
        availableNickname.addAll(
                new ArrayList<>(
                        configuration.getStringList("nicknames").stream()
                                .map(String::toLowerCase)
                                .toList()
                )
        );

        enabled = configuration.getBoolean("enabled");
    }

    public void addTemporarily(String nickname) {
        availableNickname.add(nickname.toLowerCase());
    }

    public void removeTemporarily(String nickname) {
        availableNickname.remove(nickname.toLowerCase());
    }

}
