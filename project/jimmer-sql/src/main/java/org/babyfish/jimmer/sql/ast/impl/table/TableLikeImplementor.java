package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.AstVisitor;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.table.spi.TableLike;
import org.jetbrains.annotations.NotNull;

public interface TableLikeImplementor<E> extends TableLike<E> {

    default TableLikeImplementor<?> getParent() {
        return null;
    }

    default WeakJoinHandle getWeakJoinHandle() {
        return null;
    }

    default JoinType getJoinType() {
        return JoinType.INNER;
    }

    ImmutableProp getJoinProp();

    RealTable realTable(JoinTypeMergeScope scope);

    void accept(AstVisitor visitor);

    void renderTo(@NotNull AbstractSqlBuilder<?> builder);

    AbstractMutableStatementImpl getStatement();
}
