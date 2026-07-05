package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

class SaveReturningSql {

    private SaveReturningSql() {}

    static void appendSourceTuples(
            SaveReturning returning,
            SqlBuilder builder,
            EntityCollection<DraftSpi> entities
    ) {
        boolean addComma = false;
        for (DraftSpi draft : entities) {
            if (addComma) {
                builder.sql(", ");
            } else {
                addComma = true;
            }
            builder.enter(AbstractSqlBuilder.ScopeType.TUPLE);
            appendSourceValues(returning, builder, draft);
            builder.leave();
        }
    }

    static void appendSourceValues(
            SaveReturning returning,
            SqlBuilder builder,
            DraftSpi draft
    ) {
        for (SaveReturningColumnValue sourceValue : returning.sourceValues) {
            builder.separator();
            sourceValue.appendValue(builder, draft);
        }
    }

    static void appendSourceColumns(SaveReturning returning, SqlBuilder builder) {
        for (SaveReturningColumnValue sourceValue : returning.sourceValues) {
            builder.separator().sql(sourceValue.getter);
        }
    }

    static void appendReturning(SaveReturning returning, SqlBuilder builder, String prefix) {
        boolean addComma = false;
        for (PropertyGetter getter : returning.returningGetters) {
            if (addComma) {
                builder.sql(", ");
            } else {
                addComma = true;
            }
            builder.sql(prefix).sql(getter);
        }
    }

}
