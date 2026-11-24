package club.abelix.commons.command;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.*;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class ParsedArgs {

    List<String> positional;
    Set<String> flags;
    Map<String, List<String>> options;

    private ParsedArgs(
            List<String> positional,
            Set<String> flags,
            Map<String, List<String>> options
    ) {
        this.positional = List.copyOf(positional);
        this.flags = Set.copyOf(flags);
        Map<String, List<String>> tmp = new HashMap<>();
        options.forEach((k, v) -> tmp.put(k, List.copyOf(v)));
        this.options = Collections.unmodifiableMap(tmp);
    }

    public static ParsedArgs parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ParsedArgs(List.of(), Set.of(), Map.of());
        }

        String[] tokens = raw.trim().split("\\s+");

        List<String> positional = new ArrayList<>();
        Set<String> flags = new HashSet<>();
        Map<String, List<String>> options = new HashMap<>();

        int i = 0;
        while (i < tokens.length) {
            String token = tokens[i];

            if (!token.startsWith("--")) {
                positional.add(token);
                i++;
                continue;
            }

            String keyValue = token.substring(2);
            if (keyValue.isEmpty()) {
                i++;
                continue;
            }

            String key;
            String value = null;

            int eqIndex = keyValue.indexOf('=');
            if (eqIndex >= 0) {
                key = keyValue.substring(0, eqIndex);
                value = keyValue.substring(eqIndex + 1);
            } else {
                key = keyValue;
                if (i + 1 < tokens.length && !tokens[i + 1].startsWith("--")) {
                    value = tokens[i + 1];
                    i++;
                }
            }

            if (value == null) {
                flags.add(key);
            } else {
                options.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            }

            i++;
        }

        return new ParsedArgs(positional, flags, options);
    }

    public boolean hasFlag(String name) {
        return flags.contains(name);
    }

    public Optional<String> getSingleOption(String name) {
        List<String> values = options.get(name);
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(values.get(0));
    }

    public List<String> getOptionValues(String name) {
        return options.getOrDefault(name, List.of());
    }
}
