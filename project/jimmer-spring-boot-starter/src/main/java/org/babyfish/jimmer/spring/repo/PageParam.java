package org.babyfish.jimmer.spring.repo;

public class PageParam {

    private final int index;

    private final int size;

    private PageParam(int index, int size) {
        this.index = index;
        this.size = size;
    }

    public static PageParam byIndex(int index, int size) {
        if (index < 0) {
            throw new IllegalArgumentException("index cannot be negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be positive");
        }
        return new PageParam(index, size);
    }

    public static PageParam byNo(int no, int size) {
        if (no < 0) {
            throw new IllegalArgumentException("no must be negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be positive");
        }
        return new PageParam(no - 1, size);
    }

    public int getIndex() {
        return index;
    }

    public int getSize() {
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PageParam pageParam = (PageParam) o;

        if (index != pageParam.index) return false;
        return size == pageParam.size;
    }

    @Override
    public int hashCode() {
        int result = index;
        result = 31 * result + size;
        return result;
    }

    @Override
    public String toString() {
        return "PageParam{" +
                "index=" + index +
                ", size=" + size +
                '}';
    }
}
