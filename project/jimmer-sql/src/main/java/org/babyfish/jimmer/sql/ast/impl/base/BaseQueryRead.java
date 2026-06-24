package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.impl.table.RealTable;

public final class BaseQueryRead {

    private final RealTable realBaseTable;

    private final int[] columnIndexes;

    BaseQueryRead(RealTable realBaseTable, int columnIndex) {
        this.realBaseTable = realBaseTable;
        this.columnIndexes = new int[] {columnIndex};
    }

    BaseQueryRead(RealTable realBaseTable, int[] columnIndexes) {
        this.realBaseTable = realBaseTable;
        this.columnIndexes = columnIndexes;
    }

    public RealTable getRealBaseTable() {
        return realBaseTable;
    }

    public int size() {
        return columnIndexes.length;
    }

    public int index(int index) {
        return columnIndexes[index];
    }
}
