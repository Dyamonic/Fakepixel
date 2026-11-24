package club.abelix.commons.mysql.modern;

import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.Collectors;

public final class BufferedRow extends LinkedHashMap<String, Object> {

    @NotNull
    private String formatError(@NotNull Class<?> cls, @NotNull String column) throws IllegalStateException {
        return "Column " + column + " with type " + cls.getSimpleName() + " not found in " + formatColumns();
    }

    @NotNull
    private String formatColumns() {
        return entrySet()
                .stream().map(entry -> entry.getKey() + " [" + entry.getValue().getClass().getSimpleName() + "]")
                .collect(Collectors.joining(", "));
    }

    @NotNull
    private <T> Optional<T> lookup(@NotNull String column, @NotNull Class<T> cls) {
        Object o = get(column);

        try {
            return Optional.ofNullable(cls.cast(o));
        } catch (ClassCastException e) {
            throw new IllegalStateException(formatError(cls, column));
        }
    }

    @NotNull
    public Optional<String> getString(@NotNull String column) {
        return lookup(column, String.class);
    }

    @NotNull
    public OptionalInt getInt(@NotNull String column) {
        val lookup = lookup(column, Number.class);

        return lookup.map(number -> OptionalInt.of(number.intValue())).orElseGet(OptionalInt::empty);
    }

    @NotNull
    public OptionalLong getLong(@NotNull String column) {
        val lookup = lookup(column, Number.class);

        return lookup.map(number -> OptionalLong.of(number.longValue())).orElseGet(OptionalLong::empty);
    }

    @NotNull
    public OptionalDouble getDouble(@NotNull String column) {
        val lookup = lookup(column, Number.class);

        return lookup.map(number -> OptionalDouble.of(number.longValue())).orElseGet(OptionalDouble::empty);    }

    public boolean getBoolean(@NotNull String column) {
        Object object = get(column);

        if (object instanceof Boolean) {
            return (boolean) object;
        } else if (object instanceof Number) {
            return ((Number) object).byteValue() == 1;
        } else if (object instanceof String) {
            return object.equals("true");
        }

        return false;
    }

    @NotNull
    public Optional<byte[]> getBlob(@NotNull String column) {
        return lookup(column, byte[].class);
    }

    @NotNull
    public Optional<InetAddress> getAddress(@NotNull String column) {
        try {
            InetAddress address;
            Object obj = get(column);

            switch (obj) {
                case InetAddress inetAddress -> address = inetAddress;
                case String s -> address = InetAddress.getByName(s);
                case byte[] bytes -> address = InetAddress.getByAddress(bytes);
                case Number number -> {
                    int value = number.intValue();

                    address = InetAddress.getByAddress(new byte[]{
                            (byte) ((value & 0xFF000000) >> 24),
                            (byte) ((value & 0xFF0000) >> 16),
                            (byte) ((value & 0xFF00) >> 8),
                            (byte) (value & 0xFF)
                    });
                }
                case null, default -> {
                    return Optional.empty();
                }
            }

            return Optional.of(address);
        } catch (UnknownHostException e) {
            return Optional.empty();
        }
    }

    @NotNull
    public Optional<LocalTime> getTime(@NotNull String column) {
        return lookup(column, Time.class).map(Time::toLocalTime);
    }

    @NotNull
    public Optional<LocalDateTime> getTimestamp(@NotNull String column) {
        return lookup(column, Timestamp.class).map(Timestamp::toLocalDateTime);
    }

    public int getRequiredInt(@NotNull String column) {
        return getInt(column).orElseThrow();
    }

    public byte[] getRequiredBlob(@NotNull String column) {
        return getBlob(column).orElseThrow();
    }

    public double getRequiredDouble(@NotNull String column) {
        return getDouble(column).orElseThrow();
    }

    public long getRequiredLong(@NotNull String column) {
        return getLong(column).orElseThrow();
    }

    @NotNull
    public InetAddress getRequiredAddress(@NotNull String column) {
        return getAddress(column).orElseThrow();
    }

    @NotNull
    public String getRequiredString(@NotNull String column) {
        return getString(column).orElseThrow();
    }

    @NotNull
    public LocalTime getRequiredTime(@NotNull String column) {
        return getTime(column).orElseThrow();
    }

    @NotNull
    public LocalDateTime getRequiredTimestamp(@NotNull String column) {
        return getTimestamp(column).orElseThrow();
    }

}