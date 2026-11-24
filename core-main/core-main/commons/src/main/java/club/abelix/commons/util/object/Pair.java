package club.abelix.commons.util.object;

public class Pair<A, B> {

    protected A a;
    protected B b;

    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public static <A, B> Pair<A, B> newImmutablePair(A a, B b) {
        return new Immutable<>(a, b);
    }

    public static <A, B> Pair<A, B> newMutablePair(A a, B b) {
        return new Pair<>(a, b);
    }

    public final String format(String format) {
        return String.format(format, getFirst(), getSecond());
    }

    public boolean isImmutable() {
        return false;
    }

    public synchronized A getFirst() {
        return a;
    }

    public synchronized void setFirst(A first) {
        this.a = first;
    }

    public synchronized B getSecond() {
        return b;
    }

    public synchronized void setSecond(B second) {
        this.b = second;
    }

    public Pair<A, B> asImmutable() {
        return new Immutable<>(getFirst(), getSecond());
    }

    public Pair<A, B> asMutable() {
        return this;
    }

    public boolean isMember(Object object) {
        return object == null
                ? isFirstNull() || isSecondNull()
                : object.equals(getFirst()) || object.equals(getSecond());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Pair))
            return false;

        Pair<?, ?> pair = (Pair<?, ?>) o;

        if (isFirstNull() != pair.isFirstNull() || isSecondNull() != pair.isSecondNull()) {
            return false;
        }

        return (isFirstNull() || getFirst().equals(pair.getFirst()))
                && (isSecondNull() || getSecond().equals(pair.getSecond()));
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = 31 * hash + (isFirstNull() ? 0 : getFirst().hashCode());
        hash = 31 * hash + (isSecondNull() ? 0 : getSecond().hashCode());

        return hash;
    }

    @Override
    public String toString() {
        return getFirst() + "; " + getSecond();
    }

    public boolean isFirstNull() {
        return getFirst() == null;
    }

    public boolean isSecondNull() {
        return getSecond() == null;
    }

    public void setFirstNull() {
        setFirst(null);
    }

    public void setSecondNull() {
        setSecond(null);
    }

    private static class Immutable<A, B> extends Pair<A, B> {
        public Immutable(A first, B second) {
            super(first, second);
        }

        @Override
        public boolean isImmutable() {
            return true;
        }

        @Override
        public void setFirst(A first) {
            throw new IllegalStateException("Cannot modify immutable pair");
        }

        @Override
        public void setSecond(B second) {
            throw new IllegalStateException("Cannot modify immutable pair");
        }

        @Override
        public Pair<A, B> asImmutable() {
            return this;
        }

        @Override
        public Pair<A, B> asMutable() {
            return new Pair<>(getFirst(), getSecond());
        }
    }

}
