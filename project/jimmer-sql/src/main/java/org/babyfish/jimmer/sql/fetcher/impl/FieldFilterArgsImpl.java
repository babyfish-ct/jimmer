package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.query.AbstractMutableQueryImpl;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.AssociationTableEx;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.fetcher.FieldFilterArgs;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FieldFilterArgsImpl<T extends Table<?>> implements FieldFilterArgs<T> {

    private AbstractMutableQueryImpl query;

    private T table;

    private Collection<Object> keys;

    public static <T extends Table<?>> FieldFilterArgs<T> of(
            AbstractMutableQueryImpl query,
            T table,
            Collection<Object> keys
    ) {
        return new FieldFilterArgsImpl<>(
                query,
                table,
                Objects.requireNonNull(keys, "keys cannot be null")
        );
    }

    private FieldFilterArgsImpl(
            AbstractMutableQueryImpl query,
            T table,
            Collection<Object> keys
    ) {
        this.query = query;
        this.table = table;
        this.keys = keys != null ? Collections.unmodifiableCollection(keys) : null;
    }

    @Override
    public T getTable() {
        return table;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K> K getKey() {
        if (keys.size() > 1) {
            throw new IllegalStateException(
                    "Too much keys"
            );
        }
        return (K) keys.iterator().next();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K> Collection<K> getKeys() {
        return (Collection<K>)keys;
    }

    @Override
    @OldChain
    public Sortable where(Predicate... predicates) {
        return query.where(predicates);
    }

    @Override
    @OldChain
    public Sortable orderBy(Expression<?> ... expressions) {
        return query.orderBy(expressions);
    }

    @Override
    @OldChain
    public Sortable orderBy(Order ... orders) {
        return query.orderBy(orders);
    }

    @Override
    public <X extends Table<?>, R> ConfigurableSubQuery<R> createSubQuery(
            Class<X> tableType,
            BiFunction<MutableSubQuery, X, ConfigurableSubQuery<R>> block
    ) {
        return query.createSubQuery(tableType, block);
    }

    @Override
    public <X extends Table<?>> MutableSubQuery createWildSubQuery(
            Class<X> tableType,
            BiConsumer<MutableSubQuery, X> block
    ) {
        return query.createWildSubQuery(tableType, block);
    }

    @Override
    public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>, R> ConfigurableSubQuery<R>
    createAssociationSubQuery(
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiFunction<MutableSubQuery, AssociationTableEx<SE, ST, TE, TT>, ConfigurableSubQuery<R>> block
    ) {
        return query.createAssociationSubQuery(sourceTableType, targetTableGetter, block);
    }

    @Override
    public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>, R> MutableSubQuery createAssociationWildSubQuery(
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiConsumer<MutableSubQuery, AssociationTableEx<SE, ST, TE, TT>> block
    ) {
        return query.createAssociationWildSubQuery(sourceTableType, targetTableGetter, block);
    }

    public AbstractMutableQueryImpl unwrap() {
        return query;
    }
}
