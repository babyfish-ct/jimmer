package org.babyfish.jimmer.sql.ast.table.spi;

public enum BaseTableSelectionKind {

    NON_NULL_TABLE(true, false),

    NULLABLE_TABLE(true, true),

    NON_NULL_EXPRESSION(false, false),

    NULLABLE_EXPRESSION(false, true);

    private final boolean table;

    private final boolean nullable;

    BaseTableSelectionKind(boolean table, boolean nullable) {
        this.table = table;
        this.nullable = nullable;
    }

    public boolean isTable() {
        return table;
    }

    public boolean isNullable() {
        return nullable;
    }
}
