package club.abelix.commons.mysql.modern.util;

import org.jetbrains.annotations.NotNull;

public final class TableBuilder {

    @NotNull
    private final StringBuilder builder;
    private boolean hasColumns;

    public TableBuilder(@NotNull String name) {
        builder = new StringBuilder("CREATE TABLE IF NOT EXISTS ").append(name).append('(');
    }

    @NotNull
    public TableBuilder addLine(@NotNull String column) {
        if (hasColumns) {
            builder.append(',');
        } else {
            hasColumns = true;
        }

        builder.append(column);

        return this;
    }

    @Override
    public String toString() {
        return builder.append(')').toString();
    }
}
