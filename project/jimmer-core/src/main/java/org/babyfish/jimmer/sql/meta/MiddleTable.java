package org.babyfish.jimmer.sql.meta;

import org.babyfish.jimmer.meta.LogicalDeletedInfo;

import java.util.Objects;

public class MiddleTable implements Storage {

    private final String tableName;

    private final ColumnDefinition definition;

    private final ColumnDefinition targetDefinition;

    private final boolean readonly;

    private final boolean deletionBySourcePrevented;

    private final boolean deletionByTargetPrevented;

    private final LogicalDeletedInfo logicalDeletedInfo;

    private final JoinTableFilterInfo filterInfo;

    private MiddleTable inverse;

    public MiddleTable(
            String tableName,
            ColumnDefinition definition,
            ColumnDefinition targetDefinition,
            boolean readonly,
            boolean deletionBySourcePrevented,
            boolean deletionByTargetPrevented,
            LogicalDeletedInfo logicalDeletedInfo,
            JoinTableFilterInfo filterInfo
    ) {
        this.tableName = tableName;
        this.definition = definition;
        this.targetDefinition = targetDefinition;
        this.readonly = readonly;
        this.deletionBySourcePrevented = deletionBySourcePrevented;
        this.deletionByTargetPrevented = deletionByTargetPrevented;
        this.logicalDeletedInfo = logicalDeletedInfo;
        this.filterInfo = filterInfo;
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

    public boolean isReadonly() {
        return readonly;
    }

    public boolean isDeletionBySourcePrevented() {
        return deletionBySourcePrevented;
    }

    public boolean isDeletionByTargetPrevented() {
        return deletionByTargetPrevented;
    }

    public LogicalDeletedInfo getLogicalDeletedInfo() {
        return logicalDeletedInfo;
    }

    public JoinTableFilterInfo getFilterInfo() {
        return filterInfo;
    }

    public MiddleTable getInverse() {
        MiddleTable iv = inverse;
        if (iv == null) {
            iv = new MiddleTable(
                    tableName,
                    targetDefinition,
                    definition,
                    readonly,
                    deletionByTargetPrevented,
                    deletionBySourcePrevented,
                    logicalDeletedInfo,
                    filterInfo
            );
            inverse = iv;
        }
        return iv;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MiddleTable that = (MiddleTable) o;
        return readonly == that.readonly &&
                deletionBySourcePrevented == that.deletionBySourcePrevented &&
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
                readonly,
                deletionBySourcePrevented,
                deletionByTargetPrevented
        );
    }

    @Override
    public String toString() {
        return "MiddleTable{" +
                "tableName='" + tableName + '\'' +
                ", definition=" + definition +
                ", targetDefinition=" + targetDefinition +
                ", readonly=" + readonly +
                ", deletionBySourcePrevented=" + deletionBySourcePrevented +
                ", deletionByTargetPrevented=" + deletionByTargetPrevented +
                ", logicalDeletedInfo=" + logicalDeletedInfo +
                ", filterInfo=" + filterInfo +
                ", inverse=" + inverse +
                '}';
    }
}
