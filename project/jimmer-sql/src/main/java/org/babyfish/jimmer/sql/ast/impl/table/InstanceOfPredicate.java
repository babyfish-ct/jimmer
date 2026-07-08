package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.jetbrains.annotations.NotNull;

class InstanceOfPredicate extends AbstractPredicate {

    private final Table<?> table;

    private final ImmutableType targetType;

    InstanceOfPredicate(Table<?> table, ImmutableType targetType) {
        this.table = table;
        this.targetType = targetType;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast) TableProxies.resolve(table, visitor.getAstContext()).instanceOf(targetType))
                .accept(visitor);
    }

    @Override
    public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
        ((Ast) TableProxies.resolve(table, builder.getAstContext()).instanceOf(targetType))
                .renderTo(builder);
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
