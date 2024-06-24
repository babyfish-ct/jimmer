package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.BatchSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.ComparisonPredicates;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.collection.TypedList;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class ExclusiveIdPairPredicates {

    private ExclusiveIdPairPredicates() {}

    @SuppressWarnings("unchecked")
    static void addPredicates(
            BatchSqlBuilder builder,
            List<ValueGetter> sourceGetters,
            List<ValueGetter> targetGetters
    ) {
        for (ValueGetter sourceGetter : sourceGetters) {
            builder.separator()
                    .sql(sourceGetter)
                    .sql(" = ")
                    .variable(row -> {
                        Tuple2<Object, Collection<Object>> idTuple = (Tuple2<Object, Collection<Object>>) row;
                        return sourceGetter.get(idTuple.get_1());
                    });
        }
        ValueGetter targetGetter = targetGetters.get(0);
        builder.separator()
                .sql("not ")
                .enter(AbstractSqlBuilder.ScopeType.SUB_QUERY)
                .sql(targetGetter)
                .sql(" = any(")
                .variable(row -> {
                    Tuple2<Object, Collection<Object>> idTuple = (Tuple2<Object, Collection<Object>>) row;
                    Set<Object> values = new LinkedHashSet<>();
                    for (Object value : idTuple.get_2()) {
                        values.add(targetGetter.get(value));
                    }
                    return new TypedList<>(targetGetter.metadata().getSqlTypeName(), values.toArray());
                })
                .sql(")")
                .leave();
    }

    static void addPredicates(
            SqlBuilder builder,
            List<ValueGetter> sourceGetters,
            List<ValueGetter> targetGetters,
            Object sourceId,
            Collection<?> targetIds
    ) {
        builder.separator();
        ComparisonPredicates.renderEq(
                false,
                sourceGetters,
                sourceId,
                builder
        );
        if (!targetIds.isEmpty()) {
            builder.separator();
            ComparisonPredicates.renderIn(
                    true,
                    targetGetters,
                    targetIds,
                    builder
            );
        }
    }

    static void addPredicates(
            SqlBuilder builder,
            List<ValueGetter> sourceGetters,
            List<ValueGetter> targetGetters,
            IdPairs idPairs
    ) {
        builder.separator();
        ComparisonPredicates.renderIn(
                false,
                sourceGetters,
                Tuple2.projection1(idPairs.entries()),
                builder
        );
        if (!idPairs.tuples().isEmpty()) {
            builder.separator();
            ComparisonPredicates.renderIn(
                    true,
                    ValueGetter.tupleGetters(
                            sourceGetters,
                            targetGetters
                    ),
                    idPairs.tuples(),
                    builder
            );
        }
    }
}
