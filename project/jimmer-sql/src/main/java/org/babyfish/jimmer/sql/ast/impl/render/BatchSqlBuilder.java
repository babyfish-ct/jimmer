package org.babyfish.jimmer.sql.ast.impl.render;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.impl.TupleImplementor;
import org.babyfish.jimmer.sql.ast.impl.value.PropertyGetter;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.runtime.DbLiteral;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlFormatter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class BatchSqlBuilder extends AbstractSqlBuilder<BatchSqlBuilder> {

    private final ScopeManager scopeManager = new ScopeManager();

    private final List<TemplateVariable> templateVariables = new ArrayList<>();

    private final List<Integer> variablePositions;

    private final JSqlClientImplementor sqlClient;

    public BatchSqlBuilder(JSqlClientImplementor sqlClient) {
        this(sqlClient, true);
    }

    public BatchSqlBuilder(
            JSqlClientImplementor sqlClient,
            boolean recordPosition
    ) {
        this.sqlClient = sqlClient;
        if (recordPosition && sqlClient.getSqlFormatter().isPretty()) {
            this.variablePositions = new ArrayList<>();
        } else {
            this.variablePositions = null;
        }
    }

    @Override
    public JSqlClientImplementor sqlClient() {
        return sqlClient;
    }

    public BatchSqlBuilder variable(ValueGetter getter) {
        sql(sqlClient.getDialect().jdbcParameter(getter.metadata().getSqlType()));
        templateVariables.add(new GetterVariable(getter));
        if (variablePositions != null) {
            variablePositions.add(builder.length());
        }
        return this;
    }

    public BatchSqlBuilder defaultVariable(ValueGetter getter) {
        sql(sqlClient.getDialect().jdbcParameter(getter.metadata().getSqlType()));
        templateVariables.add(new DefaultVariable(getter));
        if (variablePositions != null) {
            variablePositions.add(builder.length());
        }
        return this;
    }

    public BatchSqlBuilder variable(Function<Object, Object> getter) {
        sql("?");
        templateVariables.add(new LambdaVariable(getter));
        if (variablePositions != null) {
            variablePositions.add(builder.length());
        }
        return this;
    }

    @Override
    public BatchSqlBuilder rawVariable(Object value) {
        if (value instanceof ImmutableProp) {
            throw new IllegalArgumentException("value cannot property");
        }
        if (value == null) {
            throw new IllegalArgumentException(
                    "The \"" +
                            BatchSqlBuilder.class.getName() +
                            "\" does not accept null value"
            );
        }
        if (value instanceof TupleImplementor) {
            throw new IllegalArgumentException(
                    "The \"" +
                            BatchSqlBuilder.class.getName() +
                            "\" does not accept tuple value"
            );
        }
        if (value instanceof ImmutableSpi) {
            throw new IllegalArgumentException(
                    "The \"" +
                            BatchSqlBuilder.class.getName() +
                            "\" does not accept embeddable value"
            );
        }

        sql(
                sqlClient.getDialect().jdbcParameter(
                        value instanceof DbLiteral ?
                                ((DbLiteral)value).getType() :
                                value.getClass()
                )
        );
        this.templateVariables.add(new LiteralVariable(value));
        if (variablePositions != null) {
            variablePositions.add(builder.length());
        }
        return this;
    }

    public BatchSqlBuilder value(ImmutableProp prop) {
        if (prop.isEmbedded(EmbeddedLevel.BOTH)) {
            throw new IllegalArgumentException(
                    "The \"" +
                            BatchSqlBuilder.class.getName() +
                            "\" does not accept embeddable property \"" +
                            prop +
                            "\""
            );
        }
        return variable(PropertyGetter.propertyGetters(sqlClient, prop).get(0));
    }

    public Tuple3<String, VariableMapper, List<Integer>> build() {
        if (scopeManager.current != null) {
            throw new IllegalStateException("Internal bug: Did not leave all scopes");
        }
        return new Tuple3<>(
                builder.toString(),
                new VariableMapper(templateVariables),
                variablePositions
        );
    }

    public static class VariableMapper {

        private final List<TemplateVariable> templateVariables;

        VariableMapper(List<TemplateVariable> templateVariables) {
            this.templateVariables = templateVariables;
        }

        @SuppressWarnings("unchecked")
        public List<Object> variables(Object row) {
            List<Object> variables = new ArrayList<>(templateVariables.size());
            for (TemplateVariable templateVariable : templateVariables) {
                Object value = templateVariable.get(row);
                if (value != null) {
                    Converter<Object, ?> converter = (Converter<Object, ?>) ARRAY_CONVERTER_MAP.get(value.getClass());
                    if (converter != null) {
                        value = converter.convert(value);
                    }
                }
                variables.add(value);
            }
            return variables;
        }
    }

    private static abstract class TemplateVariable {
        abstract Object get(Object row);
    }

    private static class GetterVariable extends TemplateVariable {

        private final ValueGetter getter;

        private GetterVariable(ValueGetter getter) {
            this.getter = getter;
        }

        @Override
        Object get(Object row) {
            Object value = getter.get(row);
            if (value instanceof Collection<?> && getter.metadata().getValueProp().isScalarList()) {
                List<?> list = (List<?>) value;
                Class<?> componentType = Classes.boxTypeOf(
                        getter.metadata().getValueProp().getElementClass()
                );
                Object[] arr = (Object[]) Array.newInstance(componentType, list.size());
                return list.toArray(arr);
            }
            return value != null ? value : new DbLiteral.DbNull(getter.metadata().getSqlType());
        }
    }

    private static class DefaultVariable extends TemplateVariable {

        private final Object value;

        DefaultVariable(ValueGetter getter) {
            Object value = getter.metadata().getDefaultValue();
            this.value = value != null ? value : new DbLiteral.DbNull(getter.metadata().getSqlType());
        }

        @Override
        Object get(Object row) {
            return value;
        }
    }

    private static class LiteralVariable extends TemplateVariable {

        private final Object value;

        private LiteralVariable(Object value) {
            this.value = value;
        }

        @Override
        Object get(Object row) {
            return value;
        }
    }

    private static class LambdaVariable extends TemplateVariable {

        private final Function<Object, Object> getter;

        private LambdaVariable(Function<Object, Object> getter) {
            this.getter = getter;
        }

        @Override
        Object get(Object row) {
            return getter.apply(row);
        }
    }

    @Override
    protected ScopeManager scopeManager() {
        return scopeManager;
    }

    @Override
    protected SqlFormatter formatter() {
        return sqlClient.getSqlFormatter();
    }
}
