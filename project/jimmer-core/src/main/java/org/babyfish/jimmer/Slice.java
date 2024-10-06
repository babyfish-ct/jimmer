package org.babyfish.jimmer;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * This data structure is cheaper than {@link Page}
 * because it does not fetch row total count
 * @param <T> The row type
 *
 * @see Page
 */
public class Slice<T> {

    @NotNull
    private final List<T> rows;

    private final boolean isHead;

    private final boolean isTail;

    public Slice(@NotNull List<T> rows, boolean isHead, boolean isTail) {
        this.rows = rows;
        this.isHead = isHead;
        this.isTail = isTail;
    }

    @NotNull
    public List<T> getRows() {
        return rows;
    }

    public boolean isHead() {
        return isHead;
    }

    public boolean isTail() {
        return isTail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Slice<?> slice = (Slice<?>) o;

        if (isHead != slice.isHead) return false;
        if (isTail != slice.isTail) return false;
        return rows.equals(slice.rows);
    }

    @Override
    public int hashCode() {
        int result = rows.hashCode();
        result = 31 * result + (isHead ? 1 : 0);
        result = 31 * result + (isTail ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Slice{" +
                "rows=" + rows +
                ", isHead=" + isHead +
                ", isTail=" + isTail +
                '}';
    }
}
