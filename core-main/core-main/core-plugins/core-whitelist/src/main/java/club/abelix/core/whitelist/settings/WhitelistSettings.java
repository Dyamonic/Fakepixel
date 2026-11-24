package club.abelix.core.whitelist.settings;

import club.abelix.commons.data.permission.Group;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>WhitelistSettings creation on 21.11.2022 at 18:06</p>
 */

@AllArgsConstructor
public class WhitelistSettings {
    @Getter
    @Setter
    private String kickMessageLocalizationKey;
    @Getter
    @Setter
    private List<Integer> accessGamerIdList;
    @Getter
    @Setter
    private Group minimalAccessGroup;
    @Getter
    @Setter
    private boolean enable;
}
