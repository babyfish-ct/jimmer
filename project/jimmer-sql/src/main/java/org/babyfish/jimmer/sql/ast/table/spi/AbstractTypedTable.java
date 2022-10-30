package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.meta.impl.RedirectedProp;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl;
import org.babyfish.jimmer.sql.ast.impl.table.*;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

import java.util.function.Function;

public abstract class AbstractTypedTable<E> implements TableProxy<E> {

    private final ImmutableType immutableType;

    private final TableImplementor<E> raw;

    private final String joinDisabledReason;

    private final DelayedOperation<E> delayedOperation;

    public AbstractTypedTable(Class<E> entityType) {
        this.immutableType = ImmutableType.get(entityType);
        this.raw = null;
        this.joinDisabledReason = null;
        this.delayedOperation = null;
    }

    public AbstractTypedTable(Class<E> entityType, DelayedOperation<E> delayedOperation) {
        this.immutableType = ImmutableType.get(entityType);
        this.raw = null;
        this.joinDisabledReason = null;
        this.delayedOperation = delayedOperation;
    }

    public AbstractTypedTable(TableImplementor<E> raw, String joinDisabledReason) {
        this.immutableType = raw.getImmutableType();
        this.raw = raw;
        this.joinDisabledReason = joinDisabledReason;
        this.delayedOperation = null;
    }

    @Override
    public ImmutableType getImmutableType() {
        return immutableType;
    }

    @Override
    public Predicate eq(Table<E> other) {
        if (raw != null) {
            return raw.eq(other);
        }
        if (other.getImmutableType() != immutableType) {
            throw new IllegalArgumentException("Cannot compare tables of different types");
        }
        String idPropName = immutableType.getIdProp().getName();
        return this.<Expression<Object>>get(idPropName).eq(other.get(idPropName));
    }

    @Override
    public Predicate isNull() {
        if (raw != null) {
            return raw.isNull();
        }
        String idPropName = immutableType.getIdProp().getName();
        return this.get(idPropName).isNull();
    }

    @Override
    public Predicate isNotNull() {
        if (raw != null) {
            return raw.isNotNull();
        }
        String idPropName = immutableType.getIdProp().getName();
        return this.get(idPropName).isNotNull();
    }

    @Override
    public NumericExpression<Long> count() {
        if (raw != null) {
            return raw.count();
        }
        String idPropName = immutableType.getIdProp().getName();
        return this.get(idPropName).count();
    }

    @Override
    public NumericExpression<Long> count(boolean distinct) {
        if (raw != null) {
            return raw.count();
        }
        String idPropName = immutableType.getIdProp().getName();
        return this.get(idPropName).count();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <XE extends Expression<?>> XE get(String prop) {
        if (raw != null) {
            return raw.get(prop);
        }
        ImmutableProp immutableProp = immutableType.getProp(prop);
        return (XE)PropExpressionImpl.of(this, immutableProp);
    }

    @Override
    public <XT extends Table<?>> XT join(String prop) {
        if (raw != null) {
            beforeJoin();
            return raw.join(prop);
        }
        return TableProxies.create(
                new DelayJoin<>(
                        this,
                        immutableType.getProp(prop),
                        JoinType.INNER,
                        null
                )
        );
    }

    @Override
    public <XT extends Table<?>> XT join(String prop, JoinType joinType) {
        if (raw != null) {
            beforeJoin();
            return raw.join(prop, joinType);
        }
        return TableProxies.create(
                new DelayJoin<>(
                        this,
                        immutableType.getProp(prop),
                        joinType,
                        null
                )
        );
    }

    @Override
    public <XT extends Table<?>> XT join(String prop, JoinType joinType, ImmutableType treatedAs) {
        if (raw != null) {
            beforeJoin();
            return raw.join(prop, joinType, treatedAs);
        }
        return TableProxies.create(
                new DelayJoin<>(
                        this,
                        immutableType.getProp(prop),
                        joinType,
                        treatedAs
                )
        );
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(ImmutableProp prop) {
        if (raw != null) {
            beforeJoin();
            return raw.inverseJoin(prop);
        }
        return TableProxies.create(
                new DelayInverseJoin<>(
                        this,
                        prop,
                        JoinType.INNER
                )
        );
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(ImmutableProp prop, JoinType joinType) {
        if (raw != null) {
            beforeJoin();
            return raw.inverseJoin(prop, joinType);
        }
        return TableProxies.create(new DelayInverseJoin<>(this, prop, joinType));
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(TypedProp.Association<?, ?> prop) {
        if (raw != null) {
            beforeJoin();
            return raw.inverseJoin(prop);
        }
        return TableProxies.create(new DelayInverseJoin<>(this, prop.unwrap(), JoinType.INNER));
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(TypedProp.Association<?, ?> prop, JoinType joinType) {
        if (raw != null) {
            beforeJoin();
            return raw.inverseJoin(prop, joinType);
        }
        return TableProxies.create(new DelayInverseJoin<>(this, prop.unwrap(), joinType));
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(
            Class<XT> targetTableType,
            Function<XT, ? extends Table<?>> backPropBlock
    ) {
        if (raw != null) {
            beforeJoin();
            return raw.inverseJoin(targetTableType, backPropBlock);
        }
        return TableProxies.create(
                new DelayInverseJoin<>(
                        this,
                        ImmutableProps.join(targetTableType, backPropBlock),
                        JoinType.INNER
                )
        );
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(
            Class<XT> targetTableType,
            Function<XT, ? extends Table<?>> backPropBlock,
            JoinType joinType
    ) {
        if (raw != null) {
            beforeJoin();
            return raw.inverseJoin(targetTableType, backPropBlock, joinType);
        }
        return TableProxies.create(
                new DelayInverseJoin<>(
                        this,
                        ImmutableProps.join(targetTableType, backPropBlock),
                        joinType
                )
        );
    }

    @Override
    public Selection<E> fetch(Fetcher<E> fetcher) {
        if (raw != null) {
            return raw.fetch(fetcher);
        }
        return new FetcherSelectionImpl<E>(this, fetcher);
    }

    @SuppressWarnings("unchecked")
    @Override
    public TableEx<E> asTableEx() {
        if (this instanceof TableEx<?>) {
            return (TableEx<E>) this;
        }
        if (raw != null) {
            return raw.asTableEx();
        }
        if (delayedOperation != null) {
            return TableProxies.create(delayedOperation);
        }
        return TableProxies.create(immutableType);
    }

    @Override
    public TableImplementor<E> __unwrap() {
        return raw;
    }

    @Override
    public TableImplementor<E> __resolve(RootTableResolver resolver) {
        if (raw != null) {
            return raw;
        }
        if (delayedOperation != null) {
            return delayedOperation.resolve(resolver);
        }
        return resolver.resolveRootTable(this);
    }

    @Override
    public String __joinDisabledReason() {
        return joinDisabledReason;
    }

    @Override
    public String toString() {
        if (raw != null) {
            return raw.toString();
        }
        if (delayedOperation != null) {
            return delayedOperation.toString();
        }
        return immutableType.toString();
    }

    private void beforeJoin() {
        if (joinDisabledReason != null) {
            throw new IllegalStateException("Table join is disabled because " + joinDisabledReason);
        }
    }

    public interface DelayedOperation<E> {

        ImmutableType targetType();

        TableImplementor<E> resolve(RootTableResolver ctx);
    }

    private static class DelayJoin<E> implements DelayedOperation<E> {

        private final AbstractTypedTable<?> parent;
        private final ImmutableProp prop;
        private final JoinType joinType;
        private final ImmutableType treatedAs;

        private DelayJoin(
                AbstractTypedTable<?> parent,
                ImmutableProp prop,
                JoinType joinType,
                ImmutableType treatedAs
        ) {
            this.parent = parent;
            this.joinType = joinType;
            this.treatedAs = treatedAs;
            if (treatedAs != null) {
                this.prop = RedirectedProp.target(prop, treatedAs);
            } else {
                this.prop = prop;
            }
        }

        @Override
        public ImmutableType targetType() {
            return treatedAs != null ? treatedAs : prop.getTargetType();
        }

        @Override
        public TableImplementor<E> resolve(RootTableResolver ctx) {
            return parent.__resolve(ctx).joinImplementor(prop.getName(), joinType, treatedAs);
        }

        @Override
        public String toString() {
            return prop.toString();
        }
    }

    private static class DelayInverseJoin<E> implements DelayedOperation<E> {

        private final AbstractTypedTable<?> parent;

        private final ImmutableProp prop;

        private final JoinType joinType;

        private DelayInverseJoin(AbstractTypedTable<?> parent, ImmutableProp prop, JoinType joinType) {
            this.parent = parent;
            this.prop = prop;
            this.joinType = joinType;
        }

        @Override
        public ImmutableType targetType() {
            return prop.getDeclaringType();
        }

        @Override
        public TableImplementor<E> resolve(RootTableResolver ctx) {
            return parent.__resolve(ctx).inverseJoinImplementor(prop, joinType);
        }

        @Override
        public String toString() {
            ImmutableProp opposite = prop.getOpposite();
            if (opposite != null) {
                return opposite.toString();
            }
            return parent + "[← " + prop + ']';
        }
    }
}
