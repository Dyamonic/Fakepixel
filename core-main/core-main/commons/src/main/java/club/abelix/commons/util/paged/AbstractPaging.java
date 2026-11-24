package club.abelix.commons.util.paged;

import club.abelix.commons.util.math.MathUtil;

/**
 * @author WhileInside
 */
public abstract class AbstractPaging<Backend> {

    protected final int limit;
    protected final Backend backend;

    public AbstractPaging(Backend backend, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be more than zero");
        }

        this.backend = backend;
        this.limit = limit;
    }

    public int getLimit() {
        return limit;
    }

    public int getPages() {
        return MathUtil.divideCeil(size(), limit);
    }

    public boolean hasAnyPages() {
        return size() > 0;
    }

    public Backend getPage(int index) {
        if (index < 0) {
            return hasAnyPages() ? getPage(0) : null;
        }

        final int size = size();
        final int pageStartIdx = index * limit;

        if (pageStartIdx >= size) {
            return hasAnyPages() ? getPage(getPages() - 1) : null;
        }

        final int pageEndIdx = Math.min((index + 1) * limit, size);

        return subList(pageStartIdx, pageEndIdx);
    }

    public abstract Backend subList(int start, int end);

    public abstract int size();

}
