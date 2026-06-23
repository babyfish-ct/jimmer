package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

class DiscriminatorPredicate extends AbstractPredicate {

    private final TableImplementor<?> table;

    private final String columnName;

    private final String value;

    DiscriminatorPredicate(TableImplementor<?> table, String columnName, String value) {
        this.table = table;
        this.columnName = columnName;
        this.value = value;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        visitor.visitTableReference(
                visitor.realTableForAnalysis(table),
                null,
                false
        );
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        SqlBuilder sqlBuilder = builder.assertSimple();
        table.realTableForRender(builder).renderColumn(
                builder,
                columnName,
                false,
                null,
                null
        );
        sqlBuilder.sql(" = ").variable(value);
    }

    @Override
    protected boolean determineHasVirtualPredicate() {
        return false;
    }

    @Override
    protected Ast onResolveVirtualPredicate(AstContext ctx) {
        return this;
    }

    @Override
    public int precedence() {
        return ExpressionPrecedences.COMPARISON;
    }
}
