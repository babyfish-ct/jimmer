package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.runtime.DbLiteral;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;

import java.util.ArrayList;
import java.util.List;

class TemplateBuilder {

    private final StringBuilder builder;

    private final List<TemplateVariable> variables = new ArrayList<>();

    private final JSqlClientImplementor sqlClient;

    private final boolean pretty;

    private Scope scope;

    private boolean lineDirty;

    TemplateBuilder(JSqlClientImplementor sqlClient) {
        this.builder = new StringBuilder();
        this.sqlClient = sqlClient;
        this.pretty = sqlClient.getSqlFormatter().isPretty();
    }

    public TemplateBuilder sql(String sql) {
        append(sql);
        return this;
    }

    public TemplateBuilder enter() {
        append("(");
        this.scope = new Scope(scope);
        return this;
    }

    public TemplateBuilder leave() {
        this.scope = this.scope.parent;
        append(")");
        return this;
    }

    public TemplateBuilder separator() {
        Scope scope = this.scope;
        if (scope != null) {
            if (pretty) {
                append("\n");
            }
            if (scope.dirty) {
                append(", ");
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

    public TemplateBuilder variable(SaveShape.Item item) {
        append("?");
        variables.add(new ItemVariable(item, sqlClient));
        return this;
    }

    public TemplateBuilder defaultVariable(SaveShape.Item item) {
        append("?");
        variables.add(new DefaultVariable(item, sqlClient));
        return this;
    }

    public Tuple2<String, VariableMapper> build() {
        return new Tuple2<>(builder.toString(), new VariableMapper(variables));
    }

    private static class Scope {
        final Scope parent;
        boolean dirty;
        private Scope(Scope parent) {
            this.parent = parent;
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

        private final SaveShape.Item item;

        private final ScalarProvider<Object, Object> scalarProvider;

        private ItemVariable(SaveShape.Item item, JSqlClientImplementor sqlClient) {
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

        private DefaultVariable(SaveShape.Item item, JSqlClientImplementor sqlClient) {
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
}
