package org.babyfish.jimmer.sql.filter.impl;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.query.AbstractMutableQueryImpl;
import org.babyfish.jimmer.sql.ast.query.ConfigurableSubQuery;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.query.Order;
import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.AssociationTableEx;
import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.filter.FilterArgs;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class AbstractFilterArgsImpl<P extends Props> implements FilterArgs<P> {

    protected final Sortable sortable;

    public AbstractFilterArgsImpl(Sortable sortable) {
        this.sortable = sortable;
    }

    @Override
    public <T extends Table<?>, R> ConfigurableSubQuery<R> createSubQuery(
            Class<T> tableType,
            BiFunction<MutableSubQuery, T, ConfigurableSubQuery<R>> block
    ) {
        return sortable.createSubQuery(tableType, block);
    }

    @Override
    public <T extends Table<?>> MutableSubQuery createWildSubQuery(
            Class<T> tableType,
            BiConsumer<MutableSubQuery, T> block
    ) {
        return sortable.createWildSubQuery(tableType, block);
    }

    @Override
    public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>, R> ConfigurableSubQuery<R> createAssociationSubQuery(
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiFunction<MutableSubQuery, AssociationTableEx<SE, ST, TE, TT>, ConfigurableSubQuery<R>> block
    ) {
        return sortable.createAssociationSubQuery(sourceTableType, targetTableGetter, block);
    }

    @Override
    public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>, R> MutableSubQuery createAssociationWildSubQuery(
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiConsumer<MutableSubQuery, AssociationTableEx<SE, ST, TE, TT>> block
    ) {
        return sortable.createAssociationWildSubQuery(sourceTableType, targetTableGetter, block);
    }

    @Override
    @OldChain
    public Sortable where(Predicate... predicates) {
        return sortable.where(predicates);
    }

    @Override
    @OldChain
    public Sortable orderBy(Expression<?>... expressions) {
        return sortable.orderBy(expressions);
    }

    @Override
    @OldChain
    public Sortable orderBy(Order... orders) {
        return sortable.orderBy(orders);
    }

    public AbstractMutableQueryImpl unwrap() {
        return (AbstractMutableQueryImpl) sortable;
    }
}
