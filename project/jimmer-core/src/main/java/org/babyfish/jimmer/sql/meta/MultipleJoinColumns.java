package org.babyfish.jimmer.sql.meta;

import java.util.Map;

public class MultipleJoinColumns extends MultipleColumns {

    private static final String[] EMPTY_ARR = new String[0];

    private final String[] referencedColumnNames;

    public MultipleJoinColumns(Map<String, String> referencedColumnMap, boolean isEmbedded) {
        super(referencedColumnMap.keySet().toArray(EMPTY_ARR), isEmbedded);
        this.referencedColumnNames = referencedColumnMap.values().toArray(new String[0]);
    }

    public String referencedName(int index) {
        return referencedColumnNames[index];
    }
}
