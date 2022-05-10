package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.query.TypedSubQuery;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

public class AstVisitor {

    private SqlBuilder sqlBuilder;

    protected AstVisitor(SqlBuilder sqlBuilder) {
        this.sqlBuilder = sqlBuilder;
    }

    public final SqlBuilder getSqlBuilder() {
        if (sqlBuilder == null) {
            throw new IllegalStateException("SqlBuilder is not supported by current Ast");
        }
        return sqlBuilder;
    }

    public void visitTableReference(Table<?> table, ImmutableProp prop) {}

    public boolean visitSubQuery(TypedSubQuery<?> subQuery) {
        return true;
    }

    public void visitAggregation(String functionName, Expression<?> expression, String prefix) {}
}
