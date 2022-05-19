package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.AssociationTableEx;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.fetcher.FilterArgs;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

class FilterArgsImpl<E, T extends Table<E>> implements FilterArgs<E, T> {

    private Sortable sortable;

    private T table;

    private Object key;

    private Collection<Object> keys;

    static <E, T extends Table<E>> FilterArgs<E, T> singleLoaderArgs(
            Sortable sortable,
            T table,
            Object key
    ) {
        return new FilterArgsImpl<>(
                sortable,
                table,
                Objects.requireNonNull(key, "key cannot be null"),
                null
        );
    }

    static <E, T extends Table<E>> FilterArgs<E, T> batchLoaderArgs(
            Sortable sortable,
            T table,
            Collection<Object> keys
    ) {
        return new FilterArgsImpl<>(
                sortable,
                table,
                null,
                Objects.requireNonNull(keys, "keys cannot be null")
        );
    }

    private FilterArgsImpl(
            Sortable sortable,
            T table,
            Object key,
            Collection<Object> keys
    ) {
        this.sortable = sortable;
        this.table = table;
        this.key = key;
        this.keys = keys != null ? Collections.unmodifiableCollection(keys) : null;
    }

    @Override
    public T getTable() {
        return table;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K> K getKey() {
        if (key == null) {
            throw new IllegalStateException(
                    "Cannot get the single key in BatchDataLoader"
            );
        }
        return (K) key;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K> Collection<K> getKeys() {
        return keys != null ? (Collection<K>) keys : Collections.singleton((K) key);
    }

    @Override
    @OldChain
    public Sortable where(Predicate... predicates) {
        return sortable.where(predicates);
    }

    @Override
    @OldChain
    public Sortable orderBy(Expression<?> expression) {
        return sortable.orderBy(expression);
    }

    @Override
    @OldChain
    public Sortable orderBy(Expression<?> expression, OrderMode orderMode) {
        return sortable.orderBy(expression, orderMode);
    }

    @Override
    @OldChain
    public Sortable orderBy(Expression<?> expression, OrderMode orderMode, NullOrderMode nullOrderMode) {
        return sortable.orderBy(expression, orderMode, nullOrderMode);
    }

    @Override
    public <X extends Table<?>, R> ConfigurableTypedSubQuery<R> createSubQuery(
            Class<X> tableType,
            BiFunction<MutableSubQuery, X, ConfigurableTypedSubQuery<R>> block
    ) {
        return sortable.createSubQuery(tableType, block);
    }

    @Override
    public <X extends Table<?>> MutableSubQuery createWildSubQuery(
            Class<X> tableType,
            BiConsumer<MutableSubQuery, X> block
    ) {
        return sortable.createWildSubQuery(tableType, block);
    }

    @Override
    public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>, R> ConfigurableTypedSubQuery<R>
    createAssociationSubQuery(
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiFunction<MutableSubQuery, AssociationTableEx<SE, ST, TE, TT>, ConfigurableTypedSubQuery<R>> block
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
}
