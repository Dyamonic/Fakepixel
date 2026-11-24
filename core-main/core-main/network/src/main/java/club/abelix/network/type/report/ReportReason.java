package club.abelix.network.type.report;

import club.abelix.network.common.locale.UserLocale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportReason {

    CHEAT("45d", new String[]{"cheat", "hack", "hacking", "hacker", "killaura", "antikb", "scaffold", "fly", "cheater"}),
    INTERFERENCE("2d", new String[]{"griefing", "troll", "trolling"}),
    TEAMING("1d", new String[]{"team", "teaming"}),
    BAD_BUILDS("7d", new String[]{"build", "builds", "inappropriate", "svast", "svastika"}),
    BUG_ABUSING("3d", new String[]{"bug", "bug abuse", "abuser", "bug abusing"}),
    BAD_SKIN("12h", new String[]{"skin", "skins", "gitler"}),
    BAD_NICKNAME(null, new String[]{"nick", "nickname"}),
    BAD_PREFIX("7d", new String[]{"prefix", "prefixes", "bad", "incorrect", "wrong"});

    private final String durationString;
    private final String[] alias;

    public String getReason(UserLocale language) {
        return language.getMessage("report_reason_" + name());
    }
}
