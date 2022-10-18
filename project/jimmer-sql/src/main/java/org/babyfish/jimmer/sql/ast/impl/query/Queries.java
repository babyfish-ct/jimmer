package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.AssociationTableEx;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Queries {

    private Queries() {}

    @SuppressWarnings("unchecked")
    public static <T extends Table<?>, R> ConfigurableRootQuery<T, R> createQuery(
            JSqlClient sqlClient,
            Class<T> tableType,
            BiFunction<MutableRootQuery<T>, T, ConfigurableRootQuery<T, R>> block
    ) {
        if (TableEx.class.isAssignableFrom(tableType)) {
            throw new IllegalArgumentException("Top-level query does not support TableEx");
        }
        ImmutableType immutableType = ImmutableType.get(tableType);
        MutableRootQueryImpl<T> query = new MutableRootQueryImpl<>(
                sqlClient,
                immutableType,
                ExecutionPurpose.QUERY,
                false
        );
        ConfigurableRootQuery<T, R> typedQuery = block.apply(query, (T)query.getTable());
        query.freeze();
        return typedQuery;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Table<?>, R> ConfigurableSubQuery<R> createSubQuery(
            Filterable parent,
            Class<T> tableType,
            BiFunction<MutableSubQuery, T, ConfigurableSubQuery<R>> block
    ) {
        ImmutableType immutableType = ImmutableType.get(tableType);
        MutableSubQueryImpl query = new MutableSubQueryImpl(
                (AbstractMutableStatementImpl) parent,
                immutableType
        );
        ConfigurableSubQuery<R> typedQuery = block.apply(query, (T)query.getTable());
        query.freeze();
        return typedQuery;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Table<?>> MutableSubQuery createWildSubQuery(
            Filterable parent,
            Class<T> tableType,
            BiConsumer<MutableSubQuery, T> block
    ) {
        ImmutableType immutableType = ImmutableType.get(tableType);
        MutableSubQueryImpl query = new MutableSubQueryImpl(
                (AbstractMutableStatementImpl) parent,
                immutableType
        );
        block.accept(query, (T)query.getTable());
        query.freeze();
        return query;
    }

    @SuppressWarnings("unchecked")
    public static <R> ConfigurableRootQuery<Table<?>, R> createQuery(
            JSqlClient sqlClient,
            ImmutableType immutableType,
            BiFunction<MutableRootQuery<Table<?>>, Table<?>, ConfigurableRootQuery<Table<?>, R>> block
    ) {
        return createQuery(sqlClient, immutableType, ExecutionPurpose.QUERY, false, block);
    }

    public static <R> ConfigurableRootQuery<Table<?>, R> createQuery(
            JSqlClient sqlClient,
            ImmutableType immutableType,
            ExecutionPurpose purpose,
            boolean ignoreFilter,
            BiFunction<MutableRootQuery<Table<?>>, Table<?>, ConfigurableRootQuery<Table<?>, R>> block
    ) {
        MutableRootQueryImpl<Table<?>> query = new MutableRootQueryImpl<>(
                sqlClient,
                immutableType,
                purpose,
                ignoreFilter
        );
        ConfigurableRootQuery<Table<?>, R> typedQuery = block.apply(query, query.getTable());
        query.freeze();
        return typedQuery;
    }

    @SuppressWarnings("unchecked")
    public static <SE, ST extends Table<SE>, TE, TT extends Table<TE>, R>
    ConfigurableRootQuery<AssociationTable<SE, ST, TE, TT>, R> createAssociationQuery(
            JSqlClient sqlClient,
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiFunction<
                    MutableRootQuery<AssociationTable<SE, ST, TE, TT>>,
                    AssociationTable<SE, ST, TE, TT>,
                    ConfigurableRootQuery<AssociationTable<SE, ST, TE, TT>, R>
            > block
    ) {
        AssociationType associationType = AssociationType.of(
                ImmutableProps.join(sourceTableType, targetTableGetter)
        );
        MutableRootQueryImpl<AssociationTable<SE, ST, TE, TT>> query = new MutableRootQueryImpl<>(
                sqlClient,
                associationType,
                ExecutionPurpose.QUERY,
                false
        );
        ConfigurableRootQuery<AssociationTable<SE, ST, TE, TT>, R> typedQuery =
                block.apply(query, (AssociationTable<SE, ST, TE, TT>)query.getTable());
        query.freeze();
        return typedQuery;
    }

    @SuppressWarnings("unchecked")
    public static <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>, R>
    ConfigurableSubQuery<R> createAssociationSubQuery(
            Filterable parent,
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiFunction<
                    MutableSubQuery,
                    AssociationTableEx<SE, ST, TE, TT>,
                    ConfigurableSubQuery<R>
            > block
    ) {
        AssociationType associationType = AssociationType.of(
                ImmutableProps.join(sourceTableType, targetTableGetter)
        );
        MutableSubQueryImpl subQuery = new MutableSubQueryImpl(
                (AbstractMutableStatementImpl) parent,
                associationType
        );
        ConfigurableSubQuery<R> typedSubQuery =
                block.apply(subQuery, (AssociationTableEx<SE, ST, TE, TT>)subQuery.getTable());
        subQuery.freeze();
        return typedSubQuery;
    }

    @SuppressWarnings("unchecked")
    public static <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>, R>
    MutableSubQuery createAssociationWildSubQuery(
            Filterable parent,
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiConsumer<
                    MutableSubQuery,
                    AssociationTableEx<SE, ST, TE, TT>
            > block
    ) {
        AssociationType associationType = AssociationType.of(
                ImmutableProps.join(sourceTableType, targetTableGetter)
        );
        MutableSubQueryImpl subQuery = new MutableSubQueryImpl(
                (AbstractMutableStatementImpl) parent,
                associationType
        );
        block.accept(subQuery, (AssociationTableEx<SE, ST, TE, TT>)subQuery.getTable());
        subQuery.freeze();
        return subQuery;
    }

    public static <R>
    ConfigurableRootQuery<AssociationTable<?, ?, ?, ?>, R> createAssociationQuery(
            JSqlClient sqlClient,
            AssociationType associationType,
            BiFunction<
                    MutableRootQuery<AssociationTable<?, ?, ?, ?>>,
                    AssociationTable<?, ?, ?, ?>,
                    ConfigurableRootQuery<AssociationTable<?, ?, ?, ?>, R>
            > block
    ) {
        return createAssociationQuery(sqlClient, associationType, ExecutionPurpose.QUERY, block);
    }

    public static <R>
    ConfigurableRootQuery<AssociationTable<?, ?, ?, ?>, R> createAssociationQuery(
            JSqlClient sqlClient,
            AssociationType associationType,
            ExecutionPurpose purpose,
            BiFunction<
                    MutableRootQuery<AssociationTable<?, ?, ?, ?>>,
                    AssociationTable<?, ?, ?, ?>,
                    ConfigurableRootQuery<AssociationTable<?, ?, ?, ?>, R>
            > block
    ) {
        MutableRootQueryImpl<AssociationTable<?, ?, ?, ?>> query = new MutableRootQueryImpl<>(
                sqlClient,
                associationType,
                purpose,
                false
        );
        ConfigurableRootQuery<AssociationTable<?, ?, ?, ?>, R> typedQuery =
                block.apply(query, (AssociationTable<?, ?, ?, ?>)query.getTable());
        query.freeze();
        return typedQuery;
    }
}
