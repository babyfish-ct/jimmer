package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.impl.TupleImplementor;
import org.babyfish.jimmer.sql.ast.impl.util.BatchSqlBuilder;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.runtime.DbLiteral;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

import java.util.ArrayList;
import java.util.List;

class TemplateBuilder implements BatchSqlBuilder {

    private final StringBuilder builder;

    private final List<TemplateVariable> templateVariables = new ArrayList<>();

    final JSqlClientImplementor sqlClient;

    private final boolean pretty;

    private Scope scope;

    private boolean lineDirty;

    TemplateBuilder(JSqlClientImplementor sqlClient) {
        this.builder = new StringBuilder();
        this.sqlClient = sqlClient;
        this.pretty = sqlClient.getSqlFormatter().isPretty();
    }

    @Override
    public TemplateBuilder sql(String sql) {
        append(sql);
        return this;
    }

    public TemplateBuilder enter(ScopeType type) {
        if (!type.prefix.isEmpty()) {
            append(type.prefix);
        }
        this.scope = new Scope(scope, type);
        return this;
    }

    public TemplateBuilder leave() {
        Scope oldScope = this.scope;
        this.scope = oldScope.parent;
        if (!oldScope.type.suffix.isEmpty()) {
            append(oldScope.type.suffix);
        }
        return this;
    }

    public TemplateBuilder separator() {
        Scope scope = this.scope;
        if (scope != null) {
            if (pretty) {
                append("\n");
            }
            if (scope.dirty) {
                append(scope.type.separator);
            }
            if (pretty) {
                append("\n");
            }
        }
        return this;
    }

    private void append(String sql) {
        if (pretty && !lineDirty) {
            for (Scope scope = this.scope; scope != null; scope = scope.parent) {
                builder.append("    ");
            }
        }
        builder.append(sql);
        lineDirty = !sql.endsWith("\n");
        if (scope != null) {
            scope.dirty = true;
        }
    }

    public TemplateBuilder variable(Shape.Item item) {
        append("?");
        templateVariables.add(new ItemVariable(item, sqlClient));
        return this;
    }

    public TemplateBuilder defaultVariable(Shape.Item item) {
        append("?");
        templateVariables.add(new DefaultVariable(item, sqlClient));
        return this;
    }

    @Override
    public TemplateBuilder prop(ImmutableProp prop) {
        if (prop.isEmbedded(EmbeddedLevel.BOTH)) {
            throw new IllegalArgumentException(
                    "The \"" +
                            TemplateBuilder.class.getName() +
                            "\" does not accept embeddable property \"" +
                            prop +
                            "\""
            );
        }
        return sql(Shape.item(prop).columnName(sqlClient.getMetadataStrategy()));
    }

    @Override
    public BatchSqlBuilder value(ImmutableProp prop) {
        if (prop.isEmbedded(EmbeddedLevel.BOTH)) {
            throw new IllegalArgumentException(
                    "The \"" +
                            TemplateBuilder.class.getName() +
                            "\" does not accept embeddable property \"" +
                            prop +
                            "\""
            );
        }
        return variable(Shape.item(prop));
    }

    @Override
    public BatchSqlBuilder value(Object value) {
        if (value instanceof ImmutableProp) {
            throw new IllegalArgumentException("value cannot property");
        }
        if (value == null) {
            throw new IllegalArgumentException(
                    "The \"" +
                            TemplateBuilder.class.getName() +
                            "\" does not accept null value"
            );
        }
        if (value instanceof TupleImplementor) {
            throw new IllegalArgumentException(
                    "The \"" +
                            TemplateBuilder.class.getName() +
                            "\" does not accept tuple value"
            );
        }
        if (value instanceof ImmutableSpi) {
            throw new IllegalArgumentException(
                    "The \"" +
                            TemplateBuilder.class.getName() +
                            "\" does not accept embeddable value"
            );
        }
        append("?");
        this.templateVariables.add(new LiteralVariable(value));
        return this;
    }

    public Tuple2<String, VariableMapper> build() {
        return new Tuple2<>(builder.toString(), new VariableMapper(templateVariables));
    }

    public static enum ScopeType {
        TUPLE("(", ", ", ")"),
        SET(" set ", ", ", ""),
        WHERE(" where ", " and ", ""),
        COMMA("", ", ", ""),
        AND("", " and ", "");

        final String prefix;
        final String separator;
        final String suffix;

        ScopeType(String prefix, String separator, String suffix) {
            this.prefix = prefix;
            this.separator = separator;
            this.suffix = suffix;
        }
    }

    private static class Scope {
        final Scope parent;
        final ScopeType type;
        boolean dirty;
        private Scope(Scope parent, ScopeType type) {
            this.parent = parent;
            this.type = type;
        }
    }

    static class VariableMapper {

        private final List<TemplateVariable> templateVariables;

        VariableMapper(List<TemplateVariable> templateVariables) {
            this.templateVariables = templateVariables;
        }

        List<Object> variables(DraftSpi draft) {
            List<Object> variables = new ArrayList<>(templateVariables.size());
            for (TemplateVariable templateVariable : templateVariables) {
                variables.add(templateVariable.get(draft));
            }
            return variables;
        }
    }

    private static abstract class TemplateVariable {
        abstract Object get(DraftSpi draft);
    }

    private static class ItemVariable extends TemplateVariable {

        private final Shape.Item item;

        private final ScalarProvider<Object, Object> scalarProvider;

        private ItemVariable(Shape.Item item, JSqlClientImplementor sqlClient) {
            this.item = item;
            this.scalarProvider = sqlClient.getScalarProvider(item.deepestProp());
        }

        @Override
        Object get(DraftSpi draft) {
            Object value = item.get(draft);
            if (scalarProvider != null) {
                if (value != null) {
                    try {
                        value = scalarProvider.toSql(value);
                    } catch (Exception ex) {
                        throw new IllegalStateException(
                                "Cannot convert the value of \"" +
                                        item +
                                        "\" by the scalar provider \"" +
                                        scalarProvider +
                                        "\""
                        );
                    }
                }
                return value != null ? value : new DbLiteral.DbNull(scalarProvider.getSqlType());
            }
            return value != null ? value : new DbLiteral.DbNull(item.deepestProp().getReturnClass());
        }
    }

    private static class DefaultVariable extends TemplateVariable {

        private final Object value;

        DefaultVariable(Shape.Item item, JSqlClientImplementor sqlClient) {
            ImmutableProp prop = item.deepestProp();
            Object value = prop.getDefaultValueRef().getValue();
            ScalarProvider<Object, Object> scalarProvider = sqlClient.getScalarProvider(prop);
            if (scalarProvider != null) {
                if (value != null) {
                    try {
                        value = scalarProvider.toSql(value);
                    } catch (Exception ex) {
                        throw new IllegalStateException(
                                "Cannot convert the value of \"" +
                                        item +
                                        "\" by the scalar provider \"" +
                                        scalarProvider +
                                        "\""
                        );
                    }
                }
                if (value == null) {
                    value = new DbLiteral.DbNull(scalarProvider.getSqlType());
                }
            } else if (value == null) {
                value = new DbLiteral.DbNull(prop.getReturnClass());
            }
            this.value = value;
        }

        @Override
        Object get(DraftSpi draft) {
            return value;
        }
    }

    private static class LiteralVariable extends TemplateVariable {

        private final Object value;

        private LiteralVariable(Object value) {
            this.value = value;
        }

        @Override
        Object get(DraftSpi draft) {
            return value;
        }
    }
}
