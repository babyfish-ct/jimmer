package org.babyfish.jimmer.sql.ast.impl.table;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.meta.EmbeddedColumns;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

/**
 * Selection based on real table implementation except table wrapper
 */
public interface TableSelection {

    ImmutableType getImmutableType();

    boolean isRemote();

    default void renderSelection(
            ImmutableProp prop,
            SqlBuilder builder,
            ColumnDefinition optionalDefinition
    ) {
        renderSelection(prop, builder, optionalDefinition, true);
    }

    void renderSelection(
            ImmutableProp prop,
            SqlBuilder builder,
            ColumnDefinition optionalDefinition,
            boolean withPrefix
    );
}
