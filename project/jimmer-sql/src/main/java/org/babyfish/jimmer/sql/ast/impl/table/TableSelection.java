package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;

import java.util.function.Function;

/**
 * Selection based on real table implementation except table wrapper
 */
public interface TableSelection extends TableTypeProvider {

    boolean isRemote();

    default void renderSelection(
            ImmutableProp prop,
            boolean rawId,
            AbstractSqlBuilder<?> builder,
            ColumnDefinition optionalDefinition
    ) {
        renderSelection(prop, rawId, builder, optionalDefinition, true, null);
    }

    default void renderSelection(
            ImmutableProp prop,
            boolean rawId,
            AbstractSqlBuilder<?> builder,
            ColumnDefinition optionalDefinition,
            boolean withPrefix
    ) {
        renderSelection(prop, rawId, builder, optionalDefinition, withPrefix, null);
    }

    void renderSelection(
            ImmutableProp prop,
            boolean rawId,
            AbstractSqlBuilder<?> builder,
            ColumnDefinition optionalDefinition,
            boolean withPrefix,
            Function<Integer, String> asBlock
    );
}
