package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class Queries {

    private Queries() {}

    @SuppressWarnings("unchecked")
    public static <T extends Table<?>, R> ConfigurableTypedRootQuery<T, R> createQuery(
            SqlClient sqlClient,
            Class<T> tableType,
            BiFunction<MutableRootQuery<T>, T, ConfigurableTypedRootQuery<T, R>> block
    ) {
        ImmutableType immutableType = ImmutableType.get(tableType);
        RootMutableQueryImpl<T> query = new RootMutableQueryImpl<>(
                sqlClient,
                immutableType
        );
        ConfigurableTypedRootQuery<T, R> typedQuery = block.apply(query, (T)query.getTable());
        query.freeze();
        return typedQuery;
    }

    @SuppressWarnings("unchecked")
    public static <T extends TableEx<?>, R> ConfigurableTypedSubQuery<R> createSubQuery(
            Filterable parent,
            Class<T> tableType,
            BiFunction<MutableSubQuery, T, ConfigurableTypedSubQuery<R>> block
    ) {
        ImmutableType immutableType = ImmutableType.get(tableType);
        SubMutableQueryImpl query = new SubMutableQueryImpl(
                (AbstractMutableQueryImpl) parent,
                immutableType
        );
        ConfigurableTypedSubQuery<R> typedQuery = block.apply(query, (T)query.getTable());
        query.freeze();
        return typedQuery;
    }

    @SuppressWarnings("unchecked")
    public static <T extends TableEx<?>> MutableSubQuery createWildSubQuery(
            Filterable parent,
            Class<T> tableType,
            BiConsumer<MutableSubQuery, T> block
    ) {
        ImmutableType immutableType = ImmutableType.get(tableType);
        SubMutableQueryImpl query = new SubMutableQueryImpl(
                (AbstractMutableQueryImpl) parent,
                immutableType
        );
        block.accept(query, (T)query.getTable());
        query.freeze();
        return query;
    }

    @SuppressWarnings("unchecked")
    public static <R> ConfigurableTypedRootQuery<Table<?>, R> createQuery(
            SqlClient sqlClient,
            ImmutableType immutableType,
            BiFunction<MutableRootQuery<Table<?>>, Table<?>, ConfigurableTypedRootQuery<Table<?>, R>> block
    ) {
        RootMutableQueryImpl<Table<?>> query = new RootMutableQueryImpl<>(
                sqlClient,
                immutableType
        );
        ConfigurableTypedRootQuery<Table<?>, R> typedQuery = block.apply(query, query.getTable());
        query.freeze();
        return typedQuery;
    }
}
