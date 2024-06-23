package org.babyfish.jimmer.sql.ast.impl.render;

import org.babyfish.jimmer.sql.ast.impl.util.InList;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.collection.TypedList;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.*;

public class ComparisonPredicates {

    private ComparisonPredicates() {}

    public static void renderEq(
            boolean negative,
            List<ValueGetter> getters,
            Object value,
            SqlBuilder builder
    ) {
        boolean hasNullable = false;
        for (ValueGetter getter : getters) {
            if (getter.get(value) == null) {
                hasNullable = true;
                break;
            }
        }
        if (!hasNullable && getters.size() > 1 && builder.sqlClient().getDialect().isTupleSupported()) {
            builder.enter(AbstractSqlBuilder.ScopeType.TUPLE);
            for (ValueGetter getter : getters) {
                builder.separator().sql(getter);
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
        builder.enter(negative ? AbstractSqlBuilder.ScopeType.SMART_OR : AbstractSqlBuilder.ScopeType.AND);
        for (ValueGetter getter : getters) {
            Object v = getter.get(value);
            builder.separator().sql(getter);
            if (v == null) {
                builder.sql(negative ? " is not null" : " is null");
            } else {
                builder.sql(negative ? " <> " : " = ");
                builder.rawVariable(v);
            }
        }
        builder.leave();
    }

    public static void renderIn(
            boolean nullable,
            boolean negative,
            List<ValueGetter> getters,
            Collection<?> values,
            SqlBuilder builder
    ) {
        if (nullable) {
            renderNullableIn(negative, getters, values, builder);
        } else {
            renderIn(negative, getters, values, builder);
        }
    }

    public static void renderIn(
            boolean negative,
            List<ValueGetter> getters,
            Collection<?> values,
            SqlBuilder builder
    ) {
        if (values.isEmpty()) {
            builder.sql(negative ? "1 = 1" : "1 = 0");
            return;
        }
        JSqlClientImplementor sqlClient = builder.sqlClient();
        Dialect dialect = sqlClient.getDialect();
        if (getters.size() > 1 && !dialect.isTupleSupported()) {
            builder.enter(
                    negative ?
                            AbstractSqlBuilder.ScopeType.AND :
                            AbstractSqlBuilder.ScopeType.SMART_OR
            );
            Iterable<?> iterable = values.size() > 1 && sqlClient.isExpandedInListPaddingEnabled() ?
                    new InList<>(values, true, Integer.MAX_VALUE)
                            .iterator().next():
                    values;
            for (Object value : iterable) {
                builder.separator().enter(negative ? AbstractSqlBuilder.ScopeType.SMART_OR : AbstractSqlBuilder.ScopeType.AND);
                for (ValueGetter getter : getters) {
                    builder.separator()
                            .sql(getter)
                            .sql(negative ? " <> " : " = ")
                            .rawVariable(getter.get(value));
                }
                builder.leave();
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
                builder.sql(getter)
                        .sql(negative ? " <> " : " = ")
                        .rawVariable(getter.get(value));
                return;
            }
            builder.enter(AbstractSqlBuilder.ScopeType.TUPLE);
            for (ValueGetter getter : getters) {
                builder.separator().sql(getter);
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

            if (negative) {
                builder.sql("not ").enter(AbstractSqlBuilder.ScopeType.SUB_QUERY);
                builder.sql(getter).sql(" = any(").rawVariable(new TypedList<>(sqlType, arr)).sql(")");
                builder.leave();
            } else {
                builder.sql(getter).sql(" = any(").rawVariable(new TypedList<>(sqlType, arr)).sql(")");
            }
            return;
        }
        InList<?> inList = new InList<>(values, sqlClient.isInListPaddingEnabled(), dialect.getMaxInListSize());
        if (getters.size() == 1) {
            ValueGetter getter = getters.get(0);
            builder.enter(
                    values.size() > dialect.getMaxInListSize() ?
                            negative ? AbstractSqlBuilder.ScopeType.AND : AbstractSqlBuilder.ScopeType.SMART_OR :
                            AbstractSqlBuilder.ScopeType.NULL
            );
            for (Iterable<?> subList : inList) {
                builder.separator().sql(getter)
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
        builder.enter(
                values.size() > dialect.getMaxInListSize() ?
                        negative ? AbstractSqlBuilder.ScopeType.AND : AbstractSqlBuilder.ScopeType.SMART_OR :
                        AbstractSqlBuilder.ScopeType.NULL
        );
        for (Iterable<?> subList : inList) {
            builder.separator().enter(AbstractSqlBuilder.ScopeType.TUPLE);
            for (ValueGetter getter : getters) {
                builder.separator().sql(getter);
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

    public static void renderNullableIn(
            boolean negative,
            List<ValueGetter> getters,
            Collection<?> values,
            SqlBuilder builder
    ) {
        int maxNullColIndex = -1;
        int preNullCount = 0;
        int columnCount = getters.size();
        for (int i = 0; i < columnCount; i++) {
            ValueGetter getter = getters.get(i);
            int nullCount = 0;
            for (Object value : values) {
                if (getter.get(value) == null) {
                    nullCount++;
                }
            }
            if (nullCount > preNullCount) {
                maxNullColIndex = i;
                preNullCount = nullCount;
            }
        }

        if (maxNullColIndex == -1) {
            renderIn(
                    negative,
                    getters,
                    values,
                    builder
            );
            return;
        }

        List<ValueGetter> otherGetters = new ArrayList<>(getters.size() - 1);
        for (int i = 0; i < columnCount; i++) {
            if (i != maxNullColIndex) {
                otherGetters.add(getters.get(i));
            }
        }

        List<Object> nonNullValues = new ArrayList<>(values.size() - preNullCount);
        List<Object> nullValues = new ArrayList<>(preNullCount);
        ValueGetter nullableGetter = getters.get(maxNullColIndex);
        for (Object value : values) {
            if (nullableGetter.get(value) != null) {
                nonNullValues.add(value);
            } else {
                nullValues.add(value);
            }
        }

        builder.enter(
                nonNullValues.isEmpty() ?
                        AbstractSqlBuilder.ScopeType.NULL :
                                negative ? AbstractSqlBuilder.ScopeType.AND :
                                AbstractSqlBuilder.ScopeType.SMART_OR
        );
        if (!nonNullValues.isEmpty()) {
            builder.separator();
            renderNullableIn(
                    negative,
                    getters,
                    nonNullValues,
                    builder
            );
        }
        builder.separator();
        builder.enter(
                otherGetters.isEmpty() ?
                        AbstractSqlBuilder.ScopeType.NULL :
                        negative ?
                                AbstractSqlBuilder.ScopeType.SMART_OR :
                                AbstractSqlBuilder.ScopeType.AND
        );
        builder.separator()
                .sql(nullableGetter)
                .sql(negative ? " is not null" : " is null");
        if (!otherGetters.isEmpty()) {
            builder.separator();
            renderNullableIn(
                    negative,
                    otherGetters,
                    nullValues,
                    builder
            );
        }
        builder.leave().leave();
    }
}
