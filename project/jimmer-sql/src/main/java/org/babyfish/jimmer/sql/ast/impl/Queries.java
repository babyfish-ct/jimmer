package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.SubQueryTable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class Queries {

    private Queries() {}

    @SuppressWarnings("unchecked")
    public static <T extends Table<?>, R> ConfigurableTypedRootQuery<T, R> createQuery(
            Class<T> tableType,
            SqlClient sqlClient,
            BiFunction<MutableRootQuery<T>, T, ConfigurableTypedRootQuery<T, R>> block
    ) {
        ImmutableType immutableType = ImmutableType.tryGet(tableType);
        if (immutableType == null) {
            throw new IllegalArgumentException(
                    "Cannot get immutable type from table type \"" +
                            tableType.getName() +
                            "\""
            );
        }
        RootMutableQueryImpl<T> query = new RootMutableQueryImpl<>(
                sqlClient,
                immutableType
        );
        ConfigurableTypedRootQuery<T, R> typedQuery = block.apply(query, (T)query.getTable());
        query.freeze();
        return typedQuery;
    }

    @SuppressWarnings("unchecked")
    public static <T extends SubQueryTable<?>, R> ConfigurableTypedSubQuery<R> createSubQuery(
            Class<T> tableType,
            Filterable parent,
            BiFunction<MutableSubQuery, T, ConfigurableTypedSubQuery<R>> block
    ) {
        ImmutableType immutableType = ImmutableType.tryGet(tableType);
        if (immutableType == null) {
            throw new IllegalArgumentException(
                    "Cannot get immutable type from table type \"" +
                            tableType.getName() +
                            "\""
            );
        }
        SubMutableQueryImpl query = new SubMutableQueryImpl(
                (AbstractMutableQueryImpl) parent,
                immutableType
        );
        ConfigurableTypedSubQuery<R> typedQuery = block.apply(query, (T)query.getTable());
        query.freeze();
        return typedQuery;
    }

    @SuppressWarnings("unchecked")
    public static <T extends SubQueryTable<?>> MutableSubQuery createWildSubQuery(
            Class<T> tableType,
            Filterable parent,
            BiConsumer<MutableSubQuery, T> block
    ) {
        ImmutableType immutableType = ImmutableType.tryGet(tableType);
        if (immutableType == null) {
            throw new IllegalArgumentException(
                    "Cannot get immutable type from table type \"" +
                            tableType.getName() +
                            "\""
            );
        }
        SubMutableQueryImpl query = new SubMutableQueryImpl(
                (AbstractMutableQueryImpl) parent,
                immutableType
        );
        block.accept(query, (T)query.getTable());
        query.freeze();
        return query;
    }
}
