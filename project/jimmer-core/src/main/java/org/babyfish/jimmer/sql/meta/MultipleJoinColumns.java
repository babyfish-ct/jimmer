package org.babyfish.jimmer.sql.meta;

import java.util.Map;

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
}
