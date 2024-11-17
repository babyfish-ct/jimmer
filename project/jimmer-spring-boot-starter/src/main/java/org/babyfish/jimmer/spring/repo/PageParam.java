package org.babyfish.jimmer.spring.repo;

public class PageParam {

    private final int index;

    private final int size;

    private PageParam(int index, int size) {
        this.index = index;
        this.size = size;
    }

    /**
     * Construct page param by page index and page size
     * @param index Start from 0
     * @param size Must be greater than or equal to 0
     * @return A new page param object
     */
    public static PageParam byIndex(int index, int size) {
        if (index < 0) {
            throw new IllegalArgumentException("index cannot be negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be positive");
        }
        return new PageParam(index, size);
    }

    /**
     * Construct page param by page number and page size
     * @param no Start from 1
     * @param size Must be greater than or equal to 0
     * @return A new page param object
     */
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
