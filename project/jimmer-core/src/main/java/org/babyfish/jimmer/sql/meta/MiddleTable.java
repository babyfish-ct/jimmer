package org.babyfish.jimmer.sql.meta;

import java.util.Objects;

public class MiddleTable implements Storage {

    private final String tableName;

    private final ColumnDefinition definition;

    private final ColumnDefinition targetDefinition;

    private final boolean deletionBySourcePrevented;

    private final boolean deletionByTargetPrevented;

    private MiddleTable inverse;

    public MiddleTable(
            String tableName,
            ColumnDefinition definition,
            ColumnDefinition targetDefinition,
            boolean deletionBySourcePrevented,
            boolean deletionByTargetPrevented
    ) {
        this.tableName = tableName;
        this.definition = definition;
        this.targetDefinition = targetDefinition;
        this.deletionBySourcePrevented = deletionBySourcePrevented;
        this.deletionByTargetPrevented = deletionByTargetPrevented;
    }

    public String getTableName() {
        return tableName;
    }

    public ColumnDefinition getColumnDefinition() {
        return definition;
    }

    public ColumnDefinition getTargetColumnDefinition() {
        return targetDefinition;
    }

    public boolean isDeletionBySourcePrevented() {
        return deletionBySourcePrevented;
    }

    public boolean isDeletionByTargetPrevented() {
        return deletionByTargetPrevented;
    }

    public MiddleTable getInverse() {
        MiddleTable iv = inverse;
        if (iv == null) {
            iv = new MiddleTable(tableName, targetDefinition, definition, deletionByTargetPrevented, deletionBySourcePrevented);
            inverse = iv;
        }
        return iv;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MiddleTable that = (MiddleTable) o;
        return deletionBySourcePrevented == that.deletionBySourcePrevented &&
                deletionByTargetPrevented == that.deletionByTargetPrevented &&
                tableName.equals(that.tableName) &&
                definition.equals(that.definition) &&
                targetDefinition.equals(that.targetDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                tableName,
                definition,
                targetDefinition,
                deletionBySourcePrevented,
                deletionByTargetPrevented
        );
    }
}
