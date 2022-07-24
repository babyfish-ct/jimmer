package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.AssociationTableEx;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface Filterable {

    @OldChain
    Filterable where(Predicate...predicates);

    <T extends Table<?>, R> ConfigurableSubQuery<R> createSubQuery(
            Class<T> tableType,
            BiFunction<MutableSubQuery, T, ConfigurableSubQuery<R>> block
    );

    <T extends Table<?>> MutableSubQuery createWildSubQuery(
            Class<T> tableType,
            BiConsumer<MutableSubQuery, T> block
    );

    <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>, R>
    ConfigurableSubQuery<R> createAssociationSubQuery(
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiFunction<
                    MutableSubQuery,
                    AssociationTableEx<SE, ST, TE, TT>,
                    ConfigurableSubQuery<R>
            > block
    );

    <SE, ST extends TableEx<SE>, TE, TT extends TableEx<TE>, R>
    MutableSubQuery createAssociationWildSubQuery(
            Class<ST> sourceTableType,
            Function<ST, TT> targetTableGetter,
            BiConsumer<
                    MutableSubQuery,
                    AssociationTableEx<SE, ST, TE, TT>
            > block
    );
}
