package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.query.QueryRenderContext;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.jetbrains.annotations.NotNull;

public interface TableLikeImplementor<E> extends TableLike<E> {

    TableLikeImplementor<?> getParent();

    WeakJoinHandle getWeakJoinHandle();

    JoinType getJoinType();

    default RealTable realTable(AstContext ctx) {
        return realTable(ctx.getJoinTypeMergeScope());
    }

    default RealTable realTable(QueryRenderContext ctx) {
        RealTable realTable = realTable(ctx.getAstContext().getJoinTypeMergeScope());
        ctx.applyAliases(realTable);
        ctx.getTableAliasScope().ensureAlias(realTable);
        return realTable;
    }

    default RealTable realTableForAnalysis(QueryRenderContext ctx) {
        return realTable(ctx.getAstContext().getJoinTypeMergeScope());
    }

    RealTable realTable(JoinTypeMergeScope scope);

    void accept(AstVisitor visitor);

    void renderTo(@NotNull AbstractSqlBuilder<?> builder);

    AbstractMutableStatementImpl getStatement();

    boolean hasBaseTable();

    int getSize();

    long getOrder();
}
