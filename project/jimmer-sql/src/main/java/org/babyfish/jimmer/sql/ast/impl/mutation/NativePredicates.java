package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.impl.util.CollectionUtils;
import org.babyfish.jimmer.meta.PropId;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.Collection;

class NativePredicates {

    public static void renderPredicates(
            boolean negative,
            ColumnDefinition definition,
            Collection<?> values,
            SqlBuilder builder
    ) {
        renderPredicates(negative, null, definition, values, builder);
    }

    public static void renderPredicates(
            boolean negative,
            String alias,
            ColumnDefinition definition,
            Collection<?> values,
            SqlBuilder builder
    ) {
        String match = negative ? " <> " : " = ";
        boolean oneValue = values.size() == 1;
        if (definition.size() == 1) {
            builder.definition(alias, definition, null);
            if (values.size() == 1) {
                builder.sql(match).variable(CollectionUtils.first(values));
            } else {
                builder.sql(negative ? " not in " : " in ").enter(SqlBuilder.ScopeType.LIST);
                for (Object value : values) {
                    builder.separator();
                    builder.variable(value);
                }
                builder.leave();
            }
        } else if (builder.getAstContext().getSqlClient().getDialect().isTupleSupported()) {
            builder
                    .enter(SqlBuilder.ScopeType.TUPLE)
                    .definition(alias, definition, null)
                    .leave();
            if (oneValue) {
                Object value = CollectionUtils.first(values);
                builder
                        .sql(match)
                        .enter(SqlBuilder.ScopeType.TUPLE)
                        .variable(value)
                        .leave();
            } else {
                builder.sql(negative ? " not in " : " in ").enter(SqlBuilder.ScopeType.LIST);
                for (Object value : values) {
                    builder
                            .separator()
                            .enter(SqlBuilder.ScopeType.TUPLE)
                            .variable(value)
                            .leave();
                }
                builder.leave();
            }
        } else {
            int size = definition.size();
            if (!oneValue) {
                if (!negative) {
                    builder.sql("(").space('\n');
                }
                builder.enter(negative ? SqlBuilder.ScopeType.AND : SqlBuilder.ScopeType.OR);
            }
            for (Object value : values) {
                builder.separator();
                if (!oneValue) {
                    builder.sql("(").space('\n');
                }
                builder.enter(negative ? SqlBuilder.ScopeType.OR : SqlBuilder.ScopeType.AND);
                ImmutableSpi spi = value instanceof ImmutableSpi ? (ImmutableSpi) value : null;
                for (int i = 0; i < size; i++) {
                    builder.separator();
                    if (alias != null) {
                        builder.sql(alias).sql(".");
                    }
                    builder
                            .sql(definition.name(i))
                            .sql(match)
                            .variable(spi != null ? spi.__get(PropId.byIndex(i)) : value);
                }
                builder.leave();
                if (!oneValue) {
                    builder.space('\n').sql(")");
                }
            }
            if (!oneValue) {
                builder.leave();
                if (!negative) {
                    builder.space('\n').sql(")");
                }
            }
        }
    }

    public static void renderTuplePredicates(
            boolean negative,
            ColumnDefinition definition1,
            ColumnDefinition definition2,
            Collection<Tuple2<?, ?>> tuples,
            SqlBuilder builder
    ) {
        String match = negative ? " <> " : " = ";
        boolean oneValue = tuples.size() == 1;
        if (builder.getAstContext().getSqlClient().getDialect().isTupleSupported()) {
            builder
                    .enter(SqlBuilder.ScopeType.TUPLE)
                    .definition(definition1)
                    .separator()
                    .definition(definition2)
                    .leave();
            if (oneValue) {
                Tuple2<?, ?> tuple = CollectionUtils.first(tuples);
                builder
                        .sql(match)
                        .enter(SqlBuilder.ScopeType.TUPLE)
                        .variable(tuple.get_1())
                        .separator()
                        .variable(tuple.get_2())
                        .leave();
            } else {
                builder.sql(negative ? " not in " : " in ").enter(SqlBuilder.ScopeType.LIST);
                for (Tuple2<?, ?> tuple : tuples) {
                    builder.separator();
                    builder
                            .enter(SqlBuilder.ScopeType.TUPLE)
                            .variable(tuple.get_1())
                            .separator()
                            .variable(tuple.get_2())
                            .leave();
                }
                builder.leave();
            }
        } else {
            int size1 = definition1.size();
            int size2 = definition2.size();
            if (!oneValue) {
                if (!negative) {
                    builder.sql("(").space('\n');
                }
                builder.enter(negative ? SqlBuilder.ScopeType.AND : SqlBuilder.ScopeType.OR);
            }
            for (Tuple2<?, ?> tuple : tuples) {
                builder.separator();
                if (!oneValue) {
                    builder.sql("(").space('\n');
                }
                builder.enter(negative ? SqlBuilder.ScopeType.OR : SqlBuilder.ScopeType.AND);
                Object value = tuple.get(0);
                ImmutableSpi spi = value instanceof ImmutableSpi ? (ImmutableSpi) value : null;
                for (int i = 0; i < size1; i++) {
                    builder
                            .separator()
                            .sql(definition1.name(i))
                            .sql(match)
                            .variable(spi != null ? spi.__get(PropId.byIndex(i)) : value);
                }
                value = tuple.get(1);
                spi = value instanceof ImmutableSpi ? (ImmutableSpi) value : null;
                for (int i = 0; i < size2; i++) {
                    builder
                            .separator()
                            .sql(definition2.name(i))
                            .sql(match)
                            .variable(spi != null ? spi.__get(PropId.byIndex(i)) : value);
                }
                builder.leave();
                if (!oneValue) {
                    builder.space('\n').sql(")");
                }
            }
            if (!oneValue) {
                builder.leave();
                if (!negative) {
                    builder.space('\n').sql(")");
                }
            }
        }
    }
}