package org.babyfish.jimmer.sql.meta;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class MultipleJoinColumns extends MultipleColumns {

    private static final String[] EMPTY_ARR = new String[0];

    private final String[] referencedColumnNames;

    private final boolean isForeignKey;

    public MultipleJoinColumns(Map<String, String> referencedColumnMap, boolean isEmbedded, boolean isForeignKey) {
        super(referencedColumnMap.keySet().toArray(EMPTY_ARR), isEmbedded);
        this.referencedColumnNames = referencedColumnMap.values().toArray(new String[0]);
        this.isForeignKey = isForeignKey;
    }

    public String referencedName(int index) {
        return referencedColumnNames[index];
    }

    @Override
    public boolean isForeignKey() {
        return isForeignKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MultipleJoinColumns that = (MultipleJoinColumns) o;
        return isForeignKey == that.isForeignKey && Arrays.equals(referencedColumnNames, that.referencedColumnNames);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), isForeignKey);
        result = 31 * result + Arrays.hashCode(referencedColumnNames);
        return result;
    }

    @Override
    public String toString() {
        return "MultipleJoinColumns{" +
                "arr=" + Arrays.toString(arr) +
                ", embedded=" + embedded +
                ", referencedColumnNames=" + Arrays.toString(referencedColumnNames) +
                ", isForeignKey=" + isForeignKey +
                '}';
    }
}
