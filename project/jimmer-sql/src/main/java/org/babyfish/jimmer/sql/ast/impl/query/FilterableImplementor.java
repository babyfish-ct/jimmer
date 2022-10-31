package org.babyfish.jimmer.sql.ast.impl.query;

import org.babyfish.jimmer.sql.ast.query.ConfigurableSubQuery;
import org.babyfish.jimmer.sql.ast.query.Filterable;
import org.babyfish.jimmer.sql.ast.query.MutableSubQuery;
import org.babyfish.jimmer.sql.ast.table.AssociationTable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface FilterableImplementor extends Filterable {

    default <T extends Table<?>, R> ConfigurableSubQuery<R> createSubQuery(
            Class<T> tableType, BiFunction<MutableSubQuery, T, ConfigurableSubQuery<R>> block
    ) {
        return Queries.createSubQuery(this, tableType, block);
    }

    default <T extends Table<?>> MutableSubQuery createWildSubQuery(
            Class<T> tableType, BiConsumer<MutableSubQuery, T> block
    ) {
        return Queries.createWildSubQuery(this, tableType, block);
    }

    default <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>, R>
    ConfigurableSubQuery<R> createAssociationSubQuery(
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiFunction<MutableSubQuery, AssociationTable<SE, ST, TE, TT>, ConfigurableSubQuery<R>> block
    ) {
        return Queries.createAssociationSubQuery(this, sourceTableType, targetTableGetter, block);
    }

    default <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>, R>
    MutableSubQuery createAssociationWildSubQuery(
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiConsumer<MutableSubQuery, AssociationTable<SE, ST, TE, TT>> block
    ) {
        return Queries.createAssociationWildSubQuery(this, sourceTableType, targetTableGetter, block);
    }
}
