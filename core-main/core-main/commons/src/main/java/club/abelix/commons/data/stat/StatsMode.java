package club.abelix.commons.data.stat;

import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.Mini;
import club.abelix.network.type.mode.Uncommon;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.StringJoiner;

@Getter
@ToString(exclude = "selectQuery")
public class StatsMode {

    private static final String SELECT = "SELECT %fields% " +
            "FROM `stats`.`%table%` " +
            "WHERE `Id` = ? AND (`day` >= ? OR `day` = 0);";

    private final String table;
    private final LinkedHashMap<String, StatsField> fields;

    private String selectQuery;

    StatsMode(@NonNull String tableName) {
        this.table = tableName.toUpperCase();
        this.fields = new LinkedHashMap<>();
    }

    void addField(@NonNull StatsField field) {
        this.fields.put(field.getName(), field);
    }

    void buildSelectQuery() {
        StringJoiner joiner = new StringJoiner("`, `", "`", "`"); // `day`, `quests`, `time`

        for (String s : fields.keySet()) {
            joiner.add(s);
        }

        this.selectQuery = SELECT.replace("%fields%", joiner.toString()).replace("%table%", this.table);
    }

    public Collection<StatsField> getFields() {
        return Collections.unmodifiableCollection(fields.values());
    }

    public StatsField getField(@NonNull String field) {
        StatsField result = fields.get(field.toLowerCase());

        if (result == null) {
            System.err.println("Warning: StatsField for '" + field + "' not found in " + this);
        }

        return result;
    }

    // same as getField(), but does not print warnings
    public StatsField getOptionalField(@NonNull String field) {
        return fields.get(field.toLowerCase());
    }

    public StatsField getDay() {
        return getField("day");
    }

    public StatsField getTime() {
        return getField("time");
    }

    public String getDisplayName() {
        String table = this.table;

        Master master = Master.getByName(table);

        if (master != null) {
            return master.getDisplayName();
        }

        Mini mini = Mini.getByName(table);

        if (mini != null) {
            return mini.getDisplayName();
        }

        Uncommon uncommon = Uncommon.getByName(table);

        if (uncommon != null) {
            return uncommon.getDisplayName();
        }

        return table;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o != null && getClass() == o.getClass() && Objects.equals(table, ((StatsMode) o).table);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table);
    }

}
