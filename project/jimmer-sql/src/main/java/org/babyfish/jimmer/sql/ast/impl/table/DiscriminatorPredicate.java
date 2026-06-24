package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.Variables;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.meta.Storage;
import org.jetbrains.annotations.NotNull;

class DiscriminatorPredicate extends AbstractPredicate {

    private final TableImplementor<?> table;

    private final ImmutableProp prop;

    private final Object value;

    DiscriminatorPredicate(TableImplementor<?> table, ImmutableProp prop, Object value) {
        this.table = table;
        this.prop = prop;
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
        Storage storage = prop.getStorage(builder.getAstContext().getSqlClient().getMetadataStrategy());
        table.realTableForRender(builder).renderColumn(
                builder,
                ((SingleColumn) storage).getName(),
                false,
                null,
                null
        );
        sqlBuilder.sql(" = ").variable(Variables.process(value, prop, builder.getAstContext().getSqlClient()));
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
