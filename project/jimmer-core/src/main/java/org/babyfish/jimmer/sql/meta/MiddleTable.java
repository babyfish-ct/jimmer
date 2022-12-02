package org.babyfish.jimmer.sql.meta;

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

    public String getJoinColumnName() {
        return definition.name(0);
    }

    public String getTargetJoinColumnName() {
        return targetDefinition.name(0);
    }

    public MiddleTable getInverse() {
        MiddleTable iv = inverse;
        if (iv == null) {
            iv = new MiddleTable(tableName, targetDefinition, definition);
            inverse = iv;
        }
        return iv;
    }
}
