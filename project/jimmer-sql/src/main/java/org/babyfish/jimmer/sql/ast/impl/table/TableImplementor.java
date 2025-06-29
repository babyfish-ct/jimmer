package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public interface TableImplementor<E> extends TableEx<E>, Ast, TableSelection {

    AbstractMutableStatementImpl getStatement();

    TableImplementor<?> getParent();

    boolean isInverse();

    boolean isEmpty(Predicate<TableImplementor<?>> filter);

    boolean isRemote();

    ImmutableProp getJoinProp();

    WeakJoinHandle getWeakJoinHandle();

    JoinType getJoinType();

    RealTable realTable(JoinTypeMergeScope scope);

    TableRowCountDestructive getDestructive();

    void renderJoinAsFrom(SqlBuilder builder, TableImplementor.RenderMode mode);

    <X> PropExpression<X> get(ImmutableProp prop, boolean rawId);

    <X> TableImplementor<X> joinImplementor(ImmutableProp prop);

    <X> TableImplementor<X> joinImplementor(String prop);

    <X> TableImplementor<X> joinImplementor(ImmutableProp prop, JoinType joinType);

    <X> TableImplementor<X> joinImplementor(String prop, JoinType joinType);

    <X> TableImplementor<X> joinImplementor(ImmutableProp prop, JoinType joinType, ImmutableType treatedAs);

    <X> TableImplementor<X> joinImplementor(String prop, JoinType joinType, ImmutableType treatedAs);

    <X> TableImplementor<X> inverseJoinImplementor(ImmutableProp prop);

    <X> TableImplementor<X> inverseJoinImplementor(ImmutableProp prop, JoinType joinType);

    <X> TableImplementor<X> inverseJoinImplementor(TypedProp.Association<?, ?> prop);

    <X> TableImplementor<X> inverseJoinImplementor(TypedProp.Association<?, ?> prop, JoinType joinType);

    <X> TableImplementor<X> weakJoinImplementor(
            Class<? extends WeakJoin<?, ?>> weakJoinType,
            JoinType joinType
    );

    <X> TableImplementor<X> weakJoinImplementor(
            Class<? extends Table<?>> targetTableType,
            JoinType joinType,
            WeakJoin<?, ?> weakJoinLambda
    );

    <X> TableImplementor<X> weakJoinImplementor(WeakJoinHandle handle, JoinType joinType);

    TableImplementor<?> joinFetchImplementor(ImmutableProp prop);

    static TableImplementor<?> create(
            AbstractMutableStatementImpl statement,
            ImmutableType immutableType
    ) {
        if (immutableType instanceof AssociationType) {
            return new AssociationTableImpl<>(
                    statement,
                    (AssociationType) immutableType
            );
        }
        return new TableImpl<>(
                statement,
                immutableType,
                null,
                false,
                null,
                null,
                JoinType.INNER
        );
    }

    enum RenderMode {
        NORMAL,
        FROM_ONLY,
        WHERE_ONLY,
        DEEPER_JOIN_ONLY;
    }
}
