package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.impl.AbstractMutableStatementImpl;
import org.babyfish.jimmer.sql.ast.query.*;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Queries {

    private Queries() {}

    public static <T extends Table<?>, R> ConfigurableRootQuery<T, R> createQuery(
            JSqlClientImplementor sqlClient,
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
        return block.apply(query, query.getTable());
    }

    public static <R> ConfigurableRootQuery<Table<?>, R> createQuery(
            JSqlClientImplementor sqlClient,
            ImmutableType immutableType,
            BiFunction<MutableRootQuery<Table<?>>, Table<?>, ConfigurableRootQuery<Table<?>, R>> block
    ) {
        return createQuery(sqlClient, immutableType, ExecutionPurpose.QUERY, false, block);
    }

    public static <R> ConfigurableRootQuery<Table<?>, R> createQuery(
            JSqlClientImplementor sqlClient,
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
        return block.apply(query, query.getTable());
    }

    public static <SE, ST extends Table<SE>, TE, TT extends Table<TE>, R>
    ConfigurableRootQuery<AssociationTable<SE, ST, TE, TT>, R> createAssociationQuery(
            JSqlClientImplementor sqlClient,
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
        return block.apply(query, query.getTable());
    }

    public static <R>
    ConfigurableRootQuery<AssociationTable<?, ?, ?, ?>, R> createAssociationQuery(
            JSqlClientImplementor sqlClient,
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
        return block.apply(query, query.getTable());
    }

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
        return block.apply(query, query.getTable());
    }

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
        block.accept(query, query.getTable());
        return query;
    }

    public static <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>, R>
    ConfigurableSubQuery<R> createAssociationSubQuery(
            Filterable parent,
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiFunction<
                    MutableSubQuery,
                    AssociationTable<SE, ST, TE, TT>,
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
        return block.apply(subQuery, subQuery.getTable());
    }

    public static <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>, R>
    MutableSubQuery createAssociationWildSubQuery(
            Filterable parent,
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiConsumer<
                    MutableSubQuery,
                    AssociationTable<SE, ST, TE, TT>
                    > block
    ) {
        AssociationType associationType = AssociationType.of(
                ImmutableProps.join(sourceTableType, targetTableGetter)
        );
        MutableSubQueryImpl subQuery = new MutableSubQueryImpl(
                (AbstractMutableStatementImpl) parent,
                associationType
        );
        block.accept(subQuery, subQuery.getTable());
        return subQuery;
    }
}
