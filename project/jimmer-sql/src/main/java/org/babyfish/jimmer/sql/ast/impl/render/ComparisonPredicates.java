package org.babyfish.jimmer.sql.ast.impl.render;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.table.TableProxies;
import org.babyfish.jimmer.sql.ast.impl.util.InList;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.collection.TypedList;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.runtime.DbLiteral;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;

import java.util.*;

public class ComparisonPredicates {

    private static final Map<String, String> REVERSED_OP_MAP;

    private ComparisonPredicates() {}

    @SuppressWarnings("unchecked")
    public static void renderCmp(
            String operator,
            Expression<?> left,
            Expression<?> right,
            AbstractSqlBuilder<?> builder
    ) {
        if (left instanceof LiteralExpressionImplementor<?> && right instanceof PropExpressionImplementor<?>) {
            renderCmp(REVERSED_OP_MAP.get(operator), right, left, builder);
            return;
        }

        if (left instanceof PropExpressionImplementor<?> && right instanceof LiteralExpressionImplementor<?>) {
            PropExpressionImplementor<?> propExpressionImplementor = (PropExpressionImplementor<?>) left;
            if (propExpressionImplementor.getProp().isColumnDefinition()) {
                Object value = ((LiteralExpressionImplementor<?>) right).getValue();
                List<ValueGetter> valueGetters =
                        ValueGetter.valueGetters(builder.sqlClient(), (Expression<Object>) left, value);
                if (builder instanceof SqlBuilder) {
                    String alias = TableProxies.resolve(
                            propExpressionImplementor.getTable(),
                            ((SqlBuilder)builder).getAstContext()
                    ).realTable(
                            ((SqlBuilder) builder).getAstContext()
                    ).getFinalAlias(
                            propExpressionImplementor.getProp(),
                            propExpressionImplementor.isRawId(),
                            builder.sqlClient()
                    );

                    valueGetters = ValueGetter.alias(alias, valueGetters);
                }
                renderCmp(operator, valueGetters, value, builder);
                return;
            }
        }
        
        ((Ast) left).renderTo(builder);
        builder.sql(" ");
        builder.sql(operator);
        builder.sql(" ");
        ((Ast) right).renderTo(builder);
    }

    public static void renderCmp(
            String operator,
            List<ValueGetter> getters,
            Object value,
            AbstractSqlBuilder<?> builder
    ) {
        if (!REVERSED_OP_MAP.containsKey(operator)) {
            throw new IllegalArgumentException("Illegal operator: " + operator);
        }
        if (getters.size() > 1 && !"=".equals(operator) && !"<>".equals(operator)) {
            throw new IllegalArgumentException(
                    "The comparison predicate \"" +
                            operator +
                            "\" does not support multiple columns type"
            );
        }

        boolean hasNullable = false;
        for (ValueGetter getter : getters) {
            if (isNull(getter.get(value))) {
                hasNullable = true;
                break;
            }
        }
        if (!hasNullable && getters.size() > 1 && builder.sqlClient().getDialect().isTupleComparisonSupported()) {
            builder.enter(AbstractSqlBuilder.ScopeType.TUPLE);
            for (ValueGetter getter : getters) {
                builder.separator().sql(getter);
            }
            builder.leave();
            builder.sql(" ").sql(operator).sql(" ");
            builder.enter(AbstractSqlBuilder.ScopeType.TUPLE);
            for (ValueGetter getter : getters) {
                builder.separator().rawVariable(getter.get(value));
            }
            builder.leave();
            return;
        }

        boolean eq = "=".equals(operator);
        boolean ne = "<>".equals(operator);
        builder.enter(
                getters.size() == 1 ?
                        AbstractSqlBuilder.ScopeType.NULL :
                        ne ?
                                AbstractSqlBuilder.ScopeType.SMART_OR :
                                AbstractSqlBuilder.ScopeType.AND
        );
        for (ValueGetter getter : getters) {
            Object v = getter.get(value);
            builder.separator().sql(getter);
            if (isNull(v) && (eq || ne)) {
                builder.sql(ne ? " is not null" : " is null");
            } else {
                builder.sql(" ").sql(operator).sql(" ");
                builder.rawVariable(v);
            }
        }
        builder.leave();
    }

    public static void renderExpressionIn(
            boolean negative,
            Expression<?> expression,
            Collection<Expression<?>> expressions,
            AbstractSqlBuilder<?> builder
    ) {
        if (expressions.isEmpty()) {
            builder.sql(negative ? "1 = 1" : "1 = 0");
            return;
        }
        ExpressionImplementor<?> imp = (ExpressionImplementor<?>) expression;
        Class<?> type = imp.getType();
        for (Expression<?> expr : expressions) {
            Class<?> actualType = ((ExpressionImplementor<?>)expr).getType();
            if (type != actualType) {
                throw new IllegalArgumentException(
                        "The type of left operand is \"" +
                                type.getName() +
                                "\"" +
                                ", but there is a right operand whose type is \"" +
                                actualType.getName() +
                                "\""
                );
            }
        }
        if (expression instanceof TupleExpressionImplementor<?>) {
            TupleExpressionImplementor<?> tei = (TupleExpressionImplementor<?>) expression;
            JSqlClientImplementor sqlClient = builder.sqlClient();
            Dialect dialect = sqlClient.getDialect();
            if (tei.size() > 1 && !(
                    expressions.size() == 1 ? dialect.isTupleComparisonSupported() : dialect.isTupleSupported()
            )) {
                builder.enter(
                        negative ?
                                AbstractSqlBuilder.ScopeType.AND :
                                expressions.size() == 1 ?
                                        AbstractSqlBuilder.ScopeType.NULL :
                                        AbstractSqlBuilder.ScopeType.SMART_OR
                );
                Iterable<Expression<?>> iterable = expressions.size() > 1 && sqlClient.isExpandedInListPaddingEnabled() ?
                        new InList<>(expressions, true, Integer.MAX_VALUE)
                                .iterator().next():
                        expressions;
                int size = tei.size();
                for (Expression<?> operand : iterable) {
                    TupleExpressionImplementor<?> teiOperand = (TupleExpressionImplementor<?>) operand;
                    builder.separator().enter(negative ? AbstractSqlBuilder.ScopeType.SMART_OR : AbstractSqlBuilder.ScopeType.AND);
                    for (int i = 0; i < size; i++) {
                        builder.separator();
                        ((Ast)tei.get(i)).renderTo(builder);
                        builder.sql(negative ? " <> " : " = ");
                        ((Ast)teiOperand.get(i)).renderTo(builder);
                    }
                    builder.leave();
                }
                builder.leave();
                return;
            }
        }
        ((Ast)expression).renderTo(builder);
        if (expressions.size() == 1) {
            builder.sql(negative ? " <> " : " = ");
            Expression<?> operand;
            if (expressions instanceof List<?>) {
                operand = ((List<Expression<?>>) expressions).get(0);
            } else {
                operand = expressions.iterator().next();
            }
            ((Ast) operand).renderTo(builder);
        } else {
            builder.sql(negative ? " not in " : " in ");
            builder.enter(AbstractSqlBuilder.ScopeType.LIST);
            for (Expression<?> operand : expressions) {
                builder.separator();
                ((Ast) operand).renderTo(builder);
            }
            builder.leave();
        }
    }

    @SuppressWarnings("unchecked")
    public static void renderIn(
            boolean nullable,
            boolean negative,
            Expression<?> expression,
            Collection<?> values,
            AbstractSqlBuilder<?> builder
    ) {
        if (values.isEmpty()) {
            builder.sql(negative ? "1 = 1" : "1 = 0");
            return;
        }
        Map<List<ValueGetter>, List<Object>> multiMap = new LinkedHashMap<>();
        for (Object value : values) {
            multiMap.computeIfAbsent(
                    ValueGetter.valueGetters(builder.sqlClient(), (Expression<Object>) expression, value),
                    it -> new ArrayList<>()
            ).add(value);
        }
        if (multiMap.size() == 1) {
            Map.Entry<List<ValueGetter>, List<Object>> e = multiMap.entrySet().iterator().next();
            renderIn(
                    nullable,
                    negative,
                    e.getKey(),
                    e.getValue(),
                    builder
            );
            return;
        }
        builder.enter(negative ? AbstractSqlBuilder.ScopeType.AND : AbstractSqlBuilder.ScopeType.SMART_OR);
        for (Map.Entry<List<ValueGetter>, List<Object>> e : multiMap.entrySet()) {
            builder.separator();
            renderIn(
                    nullable,
                    negative,
                    e.getKey(),
                    e.getValue(),
                    builder
            );
        }
        builder.leave();
    }

    public static void renderIn(
            boolean nullable,
            boolean negative,
            List<ValueGetter> getters,
            Collection<?> values,
            AbstractSqlBuilder<?> builder
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
            AbstractSqlBuilder<?> builder
    ) {
        if (values.isEmpty() || getters.isEmpty()) {
            builder.sql(negative ? "1 = 1" : "1 = 0");
            return;
        }
        JSqlClientImplementor sqlClient = builder.sqlClient();
        Dialect dialect = sqlClient.getDialect();
        if (getters.size() > 1 && !(
                values.size() == 1 ? dialect.isTupleComparisonSupported() : dialect.isTupleSupported()
        )) {
            builder.enter(
                    negative ?
                            AbstractSqlBuilder.ScopeType.AND :
                            values.size() == 1 ?
                                    AbstractSqlBuilder.ScopeType.NULL :
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
                            .rawVariable(nonNull(getter.get(value)));
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
                        .rawVariable(nonNull(getter.get(value)));
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
                builder.separator().rawVariable(nonNull(getter.get(value)));
            }
            builder.leave();
            return;
        }
        if (getters.size() == 1 && dialect.isAnyEqualityOfArraySupported()) {
            ValueGetter getter = getters.get(0);
            String sqlType = getter.metadata().getSqlTypeName();
            Object[] arr = new Object[values.size()];
            int index = 0;
            for (Object value : values) {
                arr[index++] = nonNull(getter.get(value));
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
                    builder.separator().rawVariable(nonNull(getter.get(value)));
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
                    builder.separator().rawVariable(nonNull(getter.get(value)));
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
            AbstractSqlBuilder<?> builder
    ) {
        if (getters.isEmpty()) {
            builder.sql(negative ? "1 = 1" : "1 = 0");
            return;
        }
        int maxNullColIndex = -1;
        int preNullCount = 0;
        int columnCount = getters.size();
        for (int i = 0; i < columnCount; i++) {
            ValueGetter getter = getters.get(i);
            int nullCount = 0;
            for (Object value : values) {
                if (isNull(getter.get(value))) {
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
            if (!isNull(nullableGetter.get(value))) {
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

    private static Object nonNull(Object value) {
        if (value == null || value instanceof DbLiteral.DbNull) {
            throw new IllegalArgumentException(
                    "The \"in\" predicate does not accept nulls, " +
                            "please use \"nullableIn\" predicate to handle nulls"
            );
        }
        return value;
    }

    private static boolean isNull(Object value) {
        return value == null || value instanceof DbLiteral.DbNull;
    }

    static {
        Map<String, String> reversedOpMap = new HashMap<>();
        reversedOpMap.put("=", "=");
        reversedOpMap.put("<>", "<>");
        reversedOpMap.put("<", ">=");
        reversedOpMap.put("<=", ">");
        reversedOpMap.put(">", "<=");
        reversedOpMap.put(">=", "<");
        REVERSED_OP_MAP = reversedOpMap;
    }
}
