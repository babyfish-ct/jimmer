package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

class UseTableVisitor extends AstVisitor {

    UseTableVisitor(SqlBuilder sqlBuilder) {
        super(sqlBuilder);
    }

    @Override
    public void visitTableReference(Table<?> table, ImmutableProp prop) {
        SqlBuilder sqlBuilder = getSqlBuilder();
        TableImpl<?> tableImpl = TableImpl.unwrap(table);
        if (prop == null) {
            if (tableImpl.getImmutableType().getSelectableProps().size() > 1) {
                sqlBuilder.useTable(tableImpl);
            }
        } else if (prop.isId()) {
            sqlBuilder.useTable(tableImpl.getParent());
        } else {
            sqlBuilder.useTable(tableImpl);
        }
    }
}
