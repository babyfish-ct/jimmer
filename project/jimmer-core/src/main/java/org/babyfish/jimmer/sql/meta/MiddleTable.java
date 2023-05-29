package org.babyfish.jimmer.sql.meta;

import java.util.Objects;

public class MiddleTable implements Storage {

    private final String tableName;

    private final ColumnDefinition definition;

    private final ColumnDefinition targetDefinition;

    private MiddleTable inverse;

    public MiddleTable(
            String tableName,
            ColumnDefinition definition, ColumnDefinition targetDefinition) {
        this.tableName = tableName;
        this.definition = definition;
        this.targetDefinition = targetDefinition;
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

    public MiddleTable getInverse() {
        MiddleTable iv = inverse;
        if (iv == null) {
            iv = new MiddleTable(tableName, targetDefinition, definition);
            inverse = iv;
        }
        return iv;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MiddleTable that = (MiddleTable) o;
        return tableName.equals(that.tableName) && definition.equals(that.definition) && targetDefinition.equals(that.targetDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableName, definition, targetDefinition);
    }

    @Override
    public String toString() {
        return "MiddleTable{" +
                "tableName='" + tableName + '\'' +
                ", definition=" + definition +
                ", targetDefinition=" + targetDefinition +
                '}';
    }
}
