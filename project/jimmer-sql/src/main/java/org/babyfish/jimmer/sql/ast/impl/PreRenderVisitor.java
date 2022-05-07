package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

class PreRenderVisitor extends AstVisitor {

    public PreRenderVisitor(SqlBuilder sqlBuilder) {
        super(sqlBuilder);
    }

    @Override
    public void visitTableReference(Table<?> table, ImmutableProp prop) {

    }
}
