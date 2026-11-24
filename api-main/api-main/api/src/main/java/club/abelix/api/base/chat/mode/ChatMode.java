package club.abelix.api.base.chat.mode;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.data.rank.UserRanks;
import club.abelix.commons.language.Language;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public enum ChatMode {

    ALL("", new String[]{"all", "global", "a"}),
    PARTY("pchat", new String[]{"party", "p"}) {
        @Override
        protected boolean checkAvailability(@NonNull Gamer gamer) {
            if (gamer.getParty() == null) {
                gamer.sendLocalizedMessage("chat_mode_party_empty");
                return false;
            }

            return true;
        }
    },
    STAFF("sc", new String[]{"staff", "s"});

    public static final ChatMode FALLBACK = ALL;
    private static final ChatMode[] VALUES = ChatMode.values();
    private static final Map<String, ChatMode> BY_NAME = new HashMap<>();

    static {
        for (ChatMode mode : VALUES) {
            for (String alias : mode.aliases) {
                BY_NAME.put(alias.toLowerCase(), mode);
            }
        }
    }

    @Getter
    private final String command;
    private final String[] aliases;

    public static ChatMode getByName(@NonNull String name) {
        return BY_NAME.get(name.toLowerCase());
    }

    public static ChatMode getById(int id) {
        return id < 0 || id > VALUES.length - 1 ? ChatMode.ALL : VALUES[id];
    }

    public static List<ChatMode> getAvailableFor(@NonNull Gamer gamer) {
        return Arrays.stream(VALUES)
                .filter(mode -> mode.check(gamer))
                .collect(Collectors.toList());
    }

    public String getPrimaryName() {
        return aliases.length == 0 ? name() : aliases[0];
    }

    public String getLocalizedName(@NonNull Language language) {
        return language.getMessage("chat_mode_" + name() + "_name");
    }

    public final boolean check(@NonNull Gamer gamer) {
        UserRanks ranks = gamer.getRanks();

        if (!ranks.hasPermission("chat.mode." + name().toLowerCase())) {
            gamer.sendLocalizedMessage("chat_mode_no_permission");
            return false;
        }

        return checkAvailability(gamer);
    }

    protected boolean checkAvailability(@NonNull Gamer gamer) {
        return true;
    }

}
