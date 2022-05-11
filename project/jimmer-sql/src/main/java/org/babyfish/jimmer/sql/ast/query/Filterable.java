package org.babyfish.jimmer.sql.ast.query;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.table.TableEx;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public interface Filterable {

    Filterable where(Predicate...predicates);

    default Filterable orderBy(
            Expression<?> expression
    ) {
        return orderBy(
                expression,
                OrderMode.ASC,
                NullOrderMode.UNSPECIFIED
        );
    }

    default Filterable orderBy(
            Expression<?> expression,
            OrderMode orderMode
    ) {
        return orderBy(
                expression,
                orderMode,
                NullOrderMode.UNSPECIFIED
        );
    }

    Filterable orderBy(
            Expression<?> expression,
            OrderMode orderMode,
            NullOrderMode nullOrderMode
    );

    <T extends TableEx<?>, R> ConfigurableTypedSubQuery<R> createSubQuery(
            Class<T> tableType,
            BiFunction<MutableSubQuery, T, ConfigurableTypedSubQuery<R>> block
    );

    <T extends TableEx<?>> MutableSubQuery createWildSubQuery(
            Class<T> tableType,
            BiConsumer<MutableSubQuery, T> block
    );
}
