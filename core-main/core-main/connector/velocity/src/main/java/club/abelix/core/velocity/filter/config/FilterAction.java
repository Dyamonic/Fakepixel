package club.abelix.core.velocity.filter.config;

import club.abelix.core.velocity.gamer.Gamer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@RequiredArgsConstructor
@Getter
public enum FilterAction {

    MESSAGE(false, true) {
        @Override
        public String apply(Gamer gamer, String message, Set<String> matches, FilterRule filterRule) {
            StringBuilder msg = new StringBuilder();

            for (String s : filterRule.getExtra().split(";")) {
                msg.append(s).append("\n");
            }

            gamer.sendMessage(msg.substring(0, msg.length() - 2));

            return message;
        }
    },
    REPLACE(false, true) {
        @Override
        public String apply(Gamer gamer, String message, Set<String> matches, FilterRule filterRule) {
            for (String match : matches) {
                message = message.replace(match, filterRule.getExtra());
            }

            return message;
        }
    },
    LOWER(false, false) {
        @Override
        public String apply(Gamer gamer, String message, Set<String> matches, FilterRule filterRule) {
            for (String match : matches) {
                message = message.replace(match, match.toLowerCase());
            }

            return message;
        }
    },
    REPLACE_ASTERISKS(false, false) {
        @Override
        public String apply(Gamer gamer, String message, Set<String> matches, FilterRule filterRule) {
            for (String match : matches) {
                message = message.replace(match, match.replaceAll(".", "*"));
            }

            return message;
        }
    },
    REPLACE_VOWELS(false, false) {
        @Override
        public String apply(Gamer gamer, String message, Set<String> matches, FilterRule filterRule) {
            for (String match : matches) {
                message = message.replace(match, match.replaceAll("[аоуыэёеяю]", "*"));
            }

            return message;
        }
    },
    SHUFFLE(false, false) {
        @Override
        public String apply(Gamer gamer, String message, Set<String> matches, FilterRule filterRule) {
            if (!message.contains(" [shuffled]")) {
                message += " [shuffled]";
            }

            return message;
        }
    },
    SCHOOLBOY(false, false) {
        @Override
        public String apply(Gamer gamer, String message, Set<String> matches, FilterRule filterRule) {
            if (!message.contains(" [schoolboy]")) {
                message += " [schoolboy]";
            }

            return message;
        }
    };

    private final boolean blocking;
    private final boolean needExtra;

    public abstract String apply(Gamer gamer, String message, Set<String> matches, FilterRule filterRule);
}