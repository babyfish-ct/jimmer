package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.Variables;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.meta.Storage;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

class DiscriminatorPredicate extends AbstractPredicate {

    private final TableImplementor<?> table;

    private final ImmutableProp prop;

    private final List<Object> values;

    DiscriminatorPredicate(TableImplementor<?> table, ImmutableProp prop, Object value) {
        this(table, prop, Collections.singletonList(value));
    }

    DiscriminatorPredicate(TableImplementor<?> table, ImmutableProp prop, List<Object> values) {
        this.table = table;
        this.prop = prop;
        this.values = values;
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
        if (values.size() == 1) {
            sqlBuilder
                    .sql(" = ")
                    .variable(Variables.process(values.get(0), prop, builder.getAstContext().getSqlClient()));
        } else {
            sqlBuilder.sql(" in ");
            sqlBuilder.enter(SqlBuilder.ScopeType.LIST);
            for (Object value : values) {
                sqlBuilder
                        .separator()
                        .variable(Variables.process(value, prop, builder.getAstContext().getSqlClient()));
            }
            sqlBuilder.leave();
        }
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
