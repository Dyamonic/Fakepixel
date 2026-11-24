package club.abelix.commons.util.object;

public class NumberPair extends Pair<Number, Number> {
    public NumberPair(Number first, Number second) {
        super(first, second);
    }

    public static NumberPair newImmutablePair(Number a, Number b) {
        return new Immutable(a, b);
    }

    public byte getFirstByte() {
        return isFirstNull() ? 0 : getFirst().byteValue();
    }

    public void setFirstByte(byte value) {
        setFirst(value);
    }

    public byte getSecondByte() {
        return isSecondNull() ? 0 : getSecond().byteValue();
    }

    public void setSecondByte(byte value) {
        setSecond(value);
    }

    public short getFirstShort() {
        return isFirstNull() ? 0 : getFirst().shortValue();
    }

    public void setFirstShort(short value) {
        setFirst(value);
    }

    public short getSecondShort() {
        return isSecondNull() ? 0 : getSecond().shortValue();
    }

    public void setSecondShort(short value) {
        setSecond(value);
    }

    public int getFirstInt() {
        return isFirstNull() ? 0 : getFirst().intValue();
    }

    public void setFirstInt(int value) {
        setFirst(value);
    }

    public int getSecondInt() {
        return isSecondNull() ? 0 : getSecond().intValue();
    }

    public void setSecondInt(int value) {
        setSecond(value);
    }

    public long getFirstLong() {
        return isFirstNull() ? 0 : getFirst().longValue();
    }

    public void setFirstLong(long value) {
        setFirst(value);
    }

    public long getSecondLong() {
        return isSecondNull() ? 0 : getSecond().longValue();
    }

    public void setSecondLong(long value) {
        setSecond(value);
    }

    public float getFirstFloat() {
        return isFirstNull() ? 0 : getFirst().floatValue();
    }

    public void setFirstFloat(float value) {
        setFirst(value);
    }

    public float getSecondFloat() {
        return isSecondNull() ? 0 : getSecond().floatValue();
    }

    public void setSecondFloat(float value) {
        setSecond(value);
    }

    public double getFirstDouble() {
        return isFirstNull() ? 0 : getFirst().doubleValue();
    }

    public void setFirstDouble(double value) {
        setFirst(value);
    }

    public double getSecondDouble() {
        return isSecondNull() ? 0 : getSecond().doubleValue();
    }

    public void setSecondDouble(double value) {
        setSecond(value);
    }

    @Override
    public NumberPair asImmutable() {
        return new Immutable(getFirst(), getSecond());
    }

    @Override
    public NumberPair asMutable() {
        return this;
    }

    @Override
    public boolean isImmutable() {
        return false;
    }

    private static class Immutable extends NumberPair {
        public Immutable(Number first, Number second) {
            super(first, second);
        }

        @Override
        public boolean isImmutable() {
            return true;
        }

        @Override
        public void setFirst(Number first) {
            throw new IllegalStateException("Cannot modify immutable pair");
        }

        @Override
        public void setSecond(Number second) {
            throw new IllegalStateException("Cannot modify immutable pair");
        }

        @Override
        public NumberPair asImmutable() {
            return this;
        }

        @Override
        public NumberPair asMutable() {
            return new NumberPair(getFirst(), getSecond());
        }

    }
}
