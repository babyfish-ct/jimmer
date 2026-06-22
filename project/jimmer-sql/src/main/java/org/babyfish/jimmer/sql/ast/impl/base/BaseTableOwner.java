package org.babyfish.jimmer.sql.ast.impl.base;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.embedded.AbstractTypedEmbeddedPropExpression;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.impl.table.TableLikeImplementor;
import org.babyfish.jimmer.sql.ast.table.BaseTable;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.babyfish.jimmer.sql.ast.table.spi.TableProxy;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

public final class BaseTableOwner {

    final BaseTableSymbol baseTable;

    final int index;

    public BaseTableOwner(BaseTable baseTable, int index) {
        Objects.requireNonNull(baseTable, "baseTable cannot be null");
        if (baseTable instanceof BaseTableSymbol) {
            this.baseTable = (BaseTableSymbol) baseTable;
        } else {
            this.baseTable = ((BaseTableImplementor) baseTable).toSymbol();
        }
        this.index = index;
    }

    public BaseTableSymbol getBaseTable() {
        return baseTable;
    }

    public int getIndex() {
        return index;
    }

    void visitOwnerStatementChain(AstContext ctx, Runnable block) {
        visitStatementChain(
                ctx,
                baseTable.getQuery().getMutableQuery(),
                Collections.newSetFromMap(new IdentityHashMap<>()),
                block
        );
    }

    private static void visitStatementChain(
            AstContext ctx,
            AbstractMutableStatementImpl statement,
            Set<AbstractMutableStatementImpl> visiting,
            Runnable block
    ) {
        if (!visiting.add(statement)) {
            ctx.pushStatement(statement);
            try {
                block.run();
            } finally {
                ctx.popStatement();
            }
            return;
        }
        TableLikeImplementor<?> tableLikeImplementor = statement.getTableLikeImplementor();
        if (tableLikeImplementor instanceof BaseTableImplementor) {
            visitStatementChain(
                    ctx,
                    ((BaseTableImplementor) tableLikeImplementor).getQuery().firstConfigurableQuery().getMutableQuery(),
                    visiting,
                    () -> {
                        ctx.pushStatement(statement);
                        try {
                            block.run();
                        } finally {
                            ctx.popStatement();
                        }
                    }
            );
        } else {
            ctx.pushStatement(statement);
            try {
                block.run();
            } finally {
                ctx.popStatement();
            }
        }
        visiting.remove(statement);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        BaseTableOwner that = (BaseTableOwner) o;
        return index == that.index && baseTable == that.baseTable;
    }

    @Override
    public int hashCode() {
        int result = System.identityHashCode(baseTable);
        result = 31 * result + Integer.hashCode(index);
        return result;
    }

    @Override
    public String toString() {
        return "BaseTableOwner{" +
                "index=" + index +
                '}';
    }

    public static BaseTableOwner of(TableLike<?> table) {
        if (table instanceof TableProxy<?>) {
            TableProxy<?> proxy = (TableProxy<?>) table;
            return proxy.__baseTableOwner();
        }
        if (table instanceof TableImplementor<?>) {
            TableImplementor<?> tableImplementor = (TableImplementor<?>) table;
            return tableImplementor.getBaseTableOwner();
        }
        return null;
    }

    public static BaseTableOwner of(Expression<?> expression) {
        if (expression instanceof AbstractTypedEmbeddedPropExpression<?>) {
            AbstractTypedEmbeddedPropExpression<?> embeddedPropExpression = (AbstractTypedEmbeddedPropExpression<?>) expression;
            return embeddedPropExpression.__baseTableOwner();
        }
        if (expression instanceof BaseTableExpression<?>) {
            BaseTableExpression<?> baseTableExpression = (BaseTableExpression<?>) expression;
            return baseTableExpression.getBaseTableOwner();
        }
        if (expression instanceof BaseTablePropExpression<?>) {
            BaseTablePropExpression<?> baseTablePropExpression = (BaseTablePropExpression<?>) expression;
            return baseTablePropExpression.getBaseTableOwner();
        }
        return null;
    }
}
