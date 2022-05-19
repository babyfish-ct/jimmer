package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.query.ConfigurableTypedSubQuery;
import org.babyfish.jimmer.sql.ast.query.Filterable;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.table.AssociationTableEx;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.fetcher.FilterArgs;

import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

class FilterArgsImpl<E, T extends Table<E>> implements FilterArgs<E, T> {

    private Filterable filterable;

    private T table;

    private Object key;

    private Collection<Object> keys;

    static <E, T extends Table<E>> FilterArgs<E, T> batchLoaderArgs(
            Filterable filterable,
            T table,
            Collection<Object> keys
    ) {
        return new FilterArgsImpl<>(
                filterable,
                table,
                null,
                Objects.requireNonNull(keys, "keys cannot be null")
        );
    }

    private FilterArgsImpl(
            Filterable filterable,
            T table,
            Object key,
            Collection<Object> keys
    ) {
        this.filterable = filterable;
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
    public Filterable where(Predicate... predicates) {
        return filterable.where(predicates);
    }

    @Override
    public <T extends Table<?>, R> ConfigurableTypedSubQuery<R> createSubQuery(
            Class<T> tableType,
            BiFunction<MutableSubQuery, T, ConfigurableTypedSubQuery<R>> block
    ) {
        return filterable.createSubQuery(tableType, block);
    }

    @Override
    public <T extends Table<?>> MutableSubQuery createWildSubQuery(
            Class<T> tableType,
            BiConsumer<MutableSubQuery, T> block
    ) {
        return filterable.createWildSubQuery(tableType, block);
    }

    @Override
    public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>, R> ConfigurableTypedSubQuery<R>
    createAssociationSubQuery(
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiFunction<MutableSubQuery, AssociationTableEx<SE, ST, TE, TT>, ConfigurableTypedSubQuery<R>> block
    ) {
        return filterable.createAssociationSubQuery(sourceTableType, targetTableGetter, block);
    }

    @Override
    public <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>, R> MutableSubQuery createAssociationWildSubQuery(
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiConsumer<MutableSubQuery, AssociationTableEx<SE, ST, TE, TT>> block
    ) {
        return filterable.createAssociationWildSubQuery(sourceTableType, targetTableGetter, block);
    }
}
