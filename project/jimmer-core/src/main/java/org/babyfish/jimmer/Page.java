package org.babyfish.jimmer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * This data structure is expensive than {@link Slice}
 * because it fetches row total count
 * @param <T> The row type
 *
 * @see Slice
 */
public class Page<T> {

    @NotNull
    private final List<T> rows;

    private final long totalRowCount;

    private final long totalPageCount;

    @JsonCreator
    public Page(
            @JsonProperty("rows") List<T> rows,
            @JsonProperty("totalRowCount") long totalRowCount,
            @JsonProperty("totalPageCount") long totalPageCount
    ) {
        this.rows = rows != null && !rows.isEmpty() ? rows : Collections.emptyList();
        this.totalRowCount = totalRowCount;
        this.totalPageCount = totalPageCount;
    }

    @NotNull
    public List<T> getRows() {
        return rows;
    }

    public long getTotalPageCount() {
        return totalPageCount;
    }

    public long getTotalRowCount() {
        return totalRowCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Page<?> page = (Page<?>) o;

        if (totalRowCount != page.totalRowCount) return false;
        if (totalPageCount != page.totalPageCount) return false;
        return rows.equals(page.rows);
    }

    @Override
    public int hashCode() {
        int result = rows.hashCode();
        result = 31 * result + (int) (totalRowCount ^ (totalRowCount >>> 32));
        result = 31 * result + (int) (totalPageCount ^ (totalPageCount >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "Page{" +
                "rows=" + rows +
                ", totalRowCount=" + totalRowCount +
                ", totalPageCount=" + totalPageCount +
                '}';
    }
}
