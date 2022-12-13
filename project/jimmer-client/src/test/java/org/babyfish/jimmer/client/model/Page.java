package org.babyfish.jimmer.client.model;

import java.util.List;

public class Page<E> {

    private final int totalRowCount;

    private final int totalPageCount;

    private final List<E> entities;

    public Page(int totalRowCount, int totalPageCount, List<E> entities) {
        this.totalRowCount = totalRowCount;
        this.totalPageCount = totalPageCount;
        this.entities = entities;
    }

    public int getTotalRowCount() {
        return totalRowCount;
    }

    public int getTotalPageCount() {
        return totalPageCount;
    }

    public List<E> getEntities() {
        return entities;
    }

    @Override
    public String toString() {
        return "Page{" +
                "totalRowCount=" + totalRowCount +
                ", totalPageCount=" + totalPageCount +
                ", entities=" + entities +
                '}';
    }
}
