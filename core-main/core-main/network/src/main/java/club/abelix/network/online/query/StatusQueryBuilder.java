package club.abelix.network.online.query;

import club.abelix.network.online.data.FieldMatchMode;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import club.abelix.network.online.data.ServerKind;
import club.abelix.network.online.field.ServerField;
import club.abelix.network.online.field.StandardServerFields;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@FieldDefaults(level = AccessLevel.PRIVATE)
public final class StatusQueryBuilder {

    final Map<ServerField, Set<String>> equalsFilters = new HashMap<>();
    final Set<String> serverIds = new HashSet<>();
    final Map<ServerField, FieldMatchMode> matchModes = new HashMap<>();
    boolean totalOnly = false;

    public StatusQueryBuilder where(ServerField field, String value) {
        equalsFilters
                .computeIfAbsent(field, f -> new HashSet<>())
                .add(value);
        return this;
    }

    public StatusQueryBuilder whereIn(ServerField field, Collection<String> values) {
        equalsFilters
                .computeIfAbsent(field, f -> new HashSet<>())
                .addAll(values);
        return this;
    }

    public StatusQueryBuilder matchMode(ServerField field, FieldMatchMode mode) {
        matchModes.put(field, mode);
        return this;
    }

    public StatusQueryBuilder tags(Collection<String> tags) {
        whereIn(StandardServerFields.TAG, lower(tags));
        return this;
    }

    public StatusQueryBuilder tagsAll(Collection<String> tags) {
        whereIn(StandardServerFields.TAG, lower(tags));
        matchMode(StandardServerFields.TAG, FieldMatchMode.ALL);
        return this;
    }

    public StatusQueryBuilder tag(String tag) {
        return where(StandardServerFields.TAG, lower(tag));
    }

    public StatusQueryBuilder mode(String mode) {
        return where(StandardServerFields.MODE, lower(mode));
    }

    public StatusQueryBuilder modes(Collection<String> modes) {
        return whereIn(StandardServerFields.MODE, lower(modes));
    }

    public StatusQueryBuilder kind(ServerKind kind) {
        return where(StandardServerFields.KIND, lower(kind.name()));
    }

    public StatusQueryBuilder arenaState(Enum<?> state) {
        return where(StandardServerFields.ARENA_STATE, lower(state.name()));
    }

    public StatusQueryBuilder arenaState(String state) {
        return where(StandardServerFields.ARENA_STATE, state);
    }

    public StatusQueryBuilder arenaStates(Collection<? extends Enum<?>> states) {
        List<String> names = states.stream()
                .map(Enum::name)
                .toList();

        return whereIn(StandardServerFields.ARENA_STATE, lower(names));
    }

    public StatusQueryBuilder serverId(String id) {
        this.serverIds.add(lower(id));
        return this;
    }

    public StatusQueryBuilder serverIds(Collection<String> ids) {
        this.serverIds.addAll(lower(ids));
        return this;
    }

    public StatusQueryBuilder totalOnly() {
        this.totalOnly = true;
        return this;
    }

    public StatusQueryBuilder withDetails() {
        this.totalOnly = false;
        return this;
    }

    private Collection<String> lower(Collection<String> in) {
        return in.stream().map(String::toLowerCase).toList();
    }

    private String lower(String in) {
        return in.toLowerCase(Locale.ROOT);
    }

    public StatusQueryParams buildParams() {
        return new StatusQueryParams(equalsFilters, serverIds, totalOnly, matchModes);
    }

    public StatusQuery build() {
        return new RedisStatusQuery(buildParams());
    }
}
