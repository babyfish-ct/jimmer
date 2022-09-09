package org.babyfish.jimmer.sql.example.model;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class Page<E> {

    @NotNull
    private final List<E> entities;

    private final int totalRowCount;

    private final int totalPageCount;

    public Page(List<E> entities, int totalRowCount, int totalPageCount) {
        this.entities = Objects.requireNonNull(entities);
        this.totalRowCount = totalRowCount;
        this.totalPageCount = totalPageCount;
    }

    @NotNull
    public List<E> getEntities() {
        return entities;
    }

    public int getTotalRowCount() {
        return totalRowCount;
    }

    public int getTotalPageCount() {
        return totalPageCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page<?> page = (Page<?>) o;
        return totalRowCount == page.totalRowCount && totalPageCount == page.totalPageCount && entities.equals(page.entities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entities, totalRowCount, totalPageCount);
    }

    @Override
    public String toString() {
        return "Page{" +
                "entities=" + entities +
                ", totalRowCount=" + totalRowCount +
                ", totalPageCount=" + totalPageCount +
                '}';
    }
}
