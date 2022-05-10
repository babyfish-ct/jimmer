package org.babyfish.jimmer.sql.ast.impl.table;

public enum TableRowCountDestructive {

    NONE, // Left join for nullable reference, Left/Inner join for non-null reference
    BREAK_ROW_COUNT, // inner join for nullable-reference
    BREAK_REPEATABILITY // Any join for Collection
}
