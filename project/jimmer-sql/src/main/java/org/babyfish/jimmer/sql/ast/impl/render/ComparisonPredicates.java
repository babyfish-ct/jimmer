package org.babyfish.jimmer.sql.ast.impl.render;

import org.babyfish.jimmer.sql.ast.impl.util.InList;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.collection.TypedList;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.Collection;
import java.util.List;

public class ComparisonPredicates {

    private ComparisonPredicates() {}

    public static void renderIn(
            boolean negative,
            List<ValueGetter> getters,
            Collection<?> values,
            AbstractSqlBuilder<?> builder
    ) {
        if (values.isEmpty()) {
            builder.sql(negative ? "1 = 1" : "1 = 0");
            return;
        }
        JSqlClientImplementor sqlClient = builder.sqlClient();
        Dialect dialect = sqlClient.getDialect();
        if (getters.size() > 1 && !dialect.isTupleSupported()) {
            builder.enter(negative ? AbstractSqlBuilder.ScopeType.AND : AbstractSqlBuilder.ScopeType.OR);
            Iterable<?> iterable = values.size() > 1 && sqlClient.isExpandedInListPaddingEnabled() ?
                    new InList<>(values, true, Integer.MAX_VALUE)
                            .iterator().next():
                    values;
            for (Object value : iterable) {
                builder.separator().enter(AbstractSqlBuilder.ScopeType.SUB_QUERY).enter(negative ? AbstractSqlBuilder.ScopeType.OR : AbstractSqlBuilder.ScopeType.AND);
                for (ValueGetter getter : getters) {
                    builder.separator()
                            .sql(getter.columnName())
                            .sql(negative ? " <> " : " = ")
                            .rawVariable(getter.get(value));
                }
                builder.leave().leave();
            }
            builder.leave();
            return;
        }
        if (values.size() == 1) {
            Object value = values instanceof List<?> ?
                    ((List<?>) values).get(0) :
                    values.iterator().next();
            if (getters.size() == 1) {
                ValueGetter getter = getters.get(0);
                builder.sql(getter.columnName())
                        .sql(negative ? " <> " : " = ")
                        .rawVariable(getter.get(value));
                return;
            }
            builder.enter(AbstractSqlBuilder.ScopeType.TUPLE);
            for (ValueGetter getter : getters) {
                builder.separator().sql(getter.columnName());
            }
            builder.leave();
            builder.sql(negative ? " <> " : " = ");
            builder.enter(AbstractSqlBuilder.ScopeType.TUPLE);
            for (ValueGetter getter : getters) {
                builder.separator().rawVariable(getter.get(value));
            }
            builder.leave();
            return;
        }
        if (getters.size() == 1 && dialect.isAnyEqualityOfArraySupported()) {
            ValueGetter getter = getters.get(0);
            String sqlType = getter.metadata().getValueProp()
                    .<SingleColumn>getStorage(sqlClient.getMetadataStrategy())
                    .getSqlType();
            Object[] arr = new Object[values.size()];
            int index = 0;
            for (Object value : values) {
                arr[index++] = getter.get(value);
            }
            builder.sql(getter.columnName())
                    .sql(negative ? " <> any" : " = any")
                    .enter(AbstractSqlBuilder.ScopeType.SUB_QUERY)
                    .rawVariable(new TypedList<>(sqlType, arr))
                    .leave();
            return;
        }
        InList<?> inList = new InList<>(values, sqlClient.isInListPaddingEnabled(), dialect.getMaxInListSize());
        if (getters.size() == 1) {
            ValueGetter getter = getters.get(0);
            builder.enter(negative ? AbstractSqlBuilder.ScopeType.AND : AbstractSqlBuilder.ScopeType.OR);
            for (Iterable<?> subList : inList) {
                builder.separator().sql(getter.columnName())
                        .sql(negative ? " not in " : " in ")
                        .enter(AbstractSqlBuilder.ScopeType.LIST);
                for (Object value : subList) {
                    builder.separator().rawVariable(getter.get(value));
                }
                builder.leave();
            }
            builder.leave();
            return;
        }
        builder.enter(negative ? AbstractSqlBuilder.ScopeType.AND : AbstractSqlBuilder.ScopeType.OR);
        for (Iterable<?> subList : inList) {
            builder.separator().enter(AbstractSqlBuilder.ScopeType.TUPLE);
            for (ValueGetter getter : getters) {
                builder.separator().sql(getter.columnName());
            }
            builder.leave();
            builder.sql(negative ? " not in " : " in ")
                    .enter(AbstractSqlBuilder.ScopeType.LIST);
            for (Object value : subList) {
                builder.separator().enter(AbstractSqlBuilder.ScopeType.TUPLE);
                for (ValueGetter getter : getters) {
                    builder.separator().rawVariable(getter.get(value));
                }
                builder.leave();
            }
            builder.leave();
        }
        builder.leave();
    }
}
