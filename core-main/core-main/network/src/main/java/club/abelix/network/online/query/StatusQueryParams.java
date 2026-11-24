package club.abelix.network.online.query;

import club.abelix.network.online.data.FieldMatchMode;
import lombok.Value;
import club.abelix.network.online.field.ServerField;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Value
public class StatusQueryParams {

    Map<ServerField, Set<String>> equalsFilters;
    Set<String> serverIds;
    boolean totalOnly;
    Map<ServerField, FieldMatchMode> matchModes;

    public boolean hasFilters() {
        return !equalsFilters.isEmpty();
    }

    public FieldMatchMode matchMode(ServerField field) {
        return matchModes.getOrDefault(field, FieldMatchMode.ANY);
    }

    public Map<ServerField, Set<String>> getEqualsFilters() {
        return Collections.unmodifiableMap(equalsFilters);
    }
}