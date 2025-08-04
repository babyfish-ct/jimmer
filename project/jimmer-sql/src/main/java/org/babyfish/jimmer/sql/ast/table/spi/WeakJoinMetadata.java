package org.babyfish.jimmer.sql.ast.table.spi;

import org.jetbrains.annotations.Nullable;

public class WeakJoinMetadata {

    private final boolean sourceBaseTable;

    private final boolean targetBaseTable;

    private final Class<?> sourceEntityType;

    private final Class<?> targetEntityType;

    public WeakJoinMetadata(boolean sourceBaseTable, boolean targetBaseTable, Class<?> sourceEntityType, Class<?> targetEntityType) {
        this.sourceBaseTable = sourceBaseTable;
        this.targetBaseTable = targetBaseTable;
        this.sourceEntityType = sourceEntityType;
        this.targetEntityType = targetEntityType;
    }

    public boolean isSourceBaseTable() {
        return sourceBaseTable;
    }

    public boolean isTargetBaseTable() {
        return targetBaseTable;
    }

    @Nullable
    public Class<?> getSourceEntityType() {
        return sourceEntityType;
    }

    @Nullable
    public Class<?> getTargetEntityType() {
        return targetEntityType;
    }

    @Override
    public String toString() {
        return "WeakJoinMetadata{" +
                "sourceBaseTable=" + sourceBaseTable +
                ", targetBaseTable=" + targetBaseTable +
                ", sourceEntityType=" + sourceEntityType +
                ", targetEntityType=" + targetEntityType +
                '}';
    }
}
