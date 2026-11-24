package club.abelix.core.velocity.filter.config;

import club.abelix.core.velocity.filter.violation.ViolationType;
import club.abelix.core.velocity.gamer.Gamer;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class FilterRule {

    private final String name;
    private final Pattern regex;
    private final Pattern ignore;
    private final FilterAction action;
    private final String ignorePermission;
    private final String extra;

    @Setter
    private ViolationType type;

    public FilterRule(@NonNull String name,
                      @NonNull String regex,
                      String ignores,
                      @NonNull FilterAction action,
                      String extra,
                      String ignorePermission) {

        this.name = name;
        this.regex = Pattern.compile(regex);
        this.ignorePermission = ignorePermission;

        if (ignores != null && !ignores.isEmpty()) {
            this.ignore = Pattern.compile(ignores);
        } else {
            this.ignore = null;
        }

        this.action = action;
        this.extra = extra;
    }

    public Set<String> getMatches(String message) {
        Set<String> strings = new HashSet<>();
        Matcher match = regex.matcher(message);

        while (match.find()) {
            String group = match.group();

            if (ignore == null || !ignore.matcher(group).find()) {
                strings.add(group);
            }
        }

        return strings;
    }

    public boolean isValid(String message) {
        Matcher match = regex.matcher(message);

        while (match.find()) {
            String group = match.group();

            if (ignore == null || !ignore.matcher(group).find()) {
                return true;
            }
        }

        return false;
    }

    public String process(Gamer gamer, String message, Set<String> matches) {
        if (ignorePermission != null && gamer.hasPermission(ignorePermission)) {
            return message;
        }

        return action.apply(gamer, message, matches, this);
    }
}