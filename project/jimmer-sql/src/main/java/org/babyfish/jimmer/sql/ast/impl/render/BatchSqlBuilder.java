package org.babyfish.jimmer.sql.ast.impl.render;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.impl.TupleImplementor;
import org.babyfish.jimmer.sql.ast.impl.value.ValueGetter;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.meta.SingleColumn;
import org.babyfish.jimmer.sql.runtime.DbLiteral;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.SqlFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class BatchSqlBuilder extends AbstractSqlBuilder<BatchSqlBuilder> {

    private final StringBuilder builder = new StringBuilder();

    private final ScopeManager scopeManager = new ScopeManager();

    private final List<TemplateVariable> templateVariables = new ArrayList<>();

    final JSqlClientImplementor sqlClient;

    private final String jsonSuffix;

    private String propPrefix;

    public BatchSqlBuilder(JSqlClientImplementor sqlClient) {
        String jsonSuffix = sqlClient.getDialect().getJsonLiteralSuffix();
        if (jsonSuffix != null) {
            if (jsonSuffix.isEmpty()) {
                jsonSuffix = null;
            } else {
                jsonSuffix = ' ' + jsonSuffix;
            }
        }
        this.sqlClient = sqlClient;
        this.jsonSuffix = jsonSuffix;
    }

    public BatchSqlBuilder variable(ValueGetter getter) {
        sql("?");
        appendJsonSuffix(getter.metadata().isJson());
        templateVariables.add(new GetterVariable(getter));
        return this;
    }

    public BatchSqlBuilder defaultVariable(ValueGetter getter) {
        sql("?");
        appendJsonSuffix(getter.metadata().isJson());
        templateVariables.add(new DefaultVariable(getter));
        return this;
    }

    public BatchSqlBuilder variable(Function<Object, Object> getter) {
        sql("?");
        templateVariables.add(new LambdaVariable(getter));
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
        sql("?");
        this.templateVariables.add(new LiteralVariable(value));
        return this;
    }

    public BatchSqlBuilder prop(ImmutableProp prop) {
        if (prop.isEmbedded(EmbeddedLevel.BOTH)) {
            throw new IllegalArgumentException(
                    "The \"" +
                            BatchSqlBuilder.class.getName() +
                            "\" does not accept embeddable property \"" +
                            prop +
                            "\""
            );
        }
        if (propPrefix != null) {
            sql(propPrefix).sql(".");
        }
        return sql(prop.<SingleColumn>getStorage(sqlClient.getMetadataStrategy()).getName());
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
        return variable(ValueGetter.valueGetters(sqlClient, prop).get(0));
    }

    public BatchSqlBuilder withPropPrefix(String propPrefix, Runnable block) {
        if (propPrefix != null && propPrefix.isEmpty()) {
            propPrefix = null;
        }
        String oldPropPrefix = this.propPrefix;
        this.propPrefix = propPrefix;
        try {
            block.run();
        } finally {
            this.propPrefix = oldPropPrefix;
        }
        return this;
    }

    private void appendJsonSuffix(boolean isJson) {
        if (jsonSuffix != null && isJson) {
            sql(jsonSuffix);
        }
    }

    public Tuple2<String, VariableMapper> build() {
        return new Tuple2<>(builder.toString(), new VariableMapper(templateVariables));
    }



    public static class VariableMapper {

        private final List<TemplateVariable> templateVariables;

        VariableMapper(List<TemplateVariable> templateVariables) {
            this.templateVariables = templateVariables;
        }

        List<Object> variables(Object row) {
            List<Object> variables = new ArrayList<>(templateVariables.size());
            for (TemplateVariable templateVariable : templateVariables) {
                variables.add(templateVariable.get(row));
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
