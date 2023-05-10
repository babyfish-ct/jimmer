package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.TupleImplementor;
import org.babyfish.jimmer.sql.ast.impl.util.EmbeddableObjects;
import org.babyfish.jimmer.sql.ast.tuple.*;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.meta.SingleColumn;

import java.util.*;
import java.util.function.Function;

public class SqlBuilder {

    private final AstContext ctx;

    private final SqlBuilder parent;

    private final SqlFormatter formatter;

    private final StringBuilder builder = new StringBuilder();

    private final List<Object> variables = new ArrayList<>();

    private final List<Integer> variableIndices;

    private boolean indentRequired;

    private int childBuilderCount;

    private boolean terminated;

    private Scope scope;

    public SqlBuilder(AstContext ctx) {
        this.ctx = ctx;
        this.parent = null;
        this.formatter = ctx.getSqlClient().getSqlFormatter();
        if (ctx.getSqlClient().getSqlFormatter().isPretty()) {
            this.variableIndices = new ArrayList<>();
        } else {
            this.variableIndices = null;
        }
    }

    private SqlBuilder(SqlBuilder parent) {
        this.ctx = parent.ctx;
        this.parent = parent;
        this.formatter = ctx.getSqlClient().getSqlFormatter();
        if (ctx.getSqlClient().getSqlFormatter().isPretty()) {
            this.variableIndices = new ArrayList<>();
        } else {
            this.variableIndices = null;
        }
        parent.childBuilderCount++;
    }

    public AstContext getAstContext() {
        return ctx;
    }

    public SqlBuilder enter(String separator) {
        enterImpl(ScopeType.BLANK, separator);
        return this;
    }

    public SqlBuilder enter(ScopeType type) {
        enterImpl(type, null);
        return this;
    }

    private void enterImpl(ScopeType type, String separator) {
        Scope oldScope = this.scope;
        boolean ignored =
                type == ScopeType.TUPLE &&
                oldScope != null &&
                oldScope.type == ScopeType.TUPLE;
        if (!ignored) {
            part(type.prefix);
        }
        this.scope = new Scope(oldScope, type, ignored, separator);
    }

    public SqlBuilder separator() {
        Scope scope = this.scope;
        if (scope != null && scope.dirty) {
            boolean forceInLine = false;
            if (scope.type == ScopeType.LIST) {
                forceInLine = ++scope.listSeparatorCount < formatter.getListParamCountInLine();
                if (!forceInLine) {
                    scope.listSeparatorCount = 0;
                }
            }
            if (scope.type.isSeparatorIndent) {
                part(scope.separator, forceInLine);
            } else {
                scope.depth--;
                part(scope.separator, forceInLine);
                scope.depth++;
            }
            scope.dirty = false;
        }
        return this;
    }

    public SqlBuilder leave() {
        Scope scope = this.scope;
        this.scope = scope.parent;
        if (!scope.ignored) {
            part(scope.type.suffix);
        }
        return this;
    }

    private void part(ScopeType.Part part) {
        part(part, false);
    }

    private void part(ScopeType.Part part, boolean forceInLine) {
        if (part == null) {
            return;
        }
        space(part.before, forceInLine);
        preAppend();
        builder.append(part.value);
        space(part.after, forceInLine);
    }

    public SqlBuilder space(char ch) {
        return space(ch, false);
    }

    public SqlBuilder space(char ch, boolean forceInLine) {
        switch (ch) {
            case '?':
                if (!forceInLine && formatter.isPretty()) {
                    newLine();
                } else {
                    preAppend();
                    builder.append(' ');
                }
                break;
            case ' ':
                preAppend();
                builder.append(' ');
                break;
            case '\n':
                if (!forceInLine && formatter.isPretty()) {
                    newLine();
                }
                break;
        }
        return this;
    }

    private void preAppend() {
        Scope scope = this.scope;
        if (scope == null) {
            SqlBuilder parent = this.parent;
            if (parent != null) {
                scope = parent.scope;
            }
        }
        if (scope != null) {
            scope.setDirty();
        }
        if (scope == null || !indentRequired) {
            indentRequired = false;
            return;
        }
        indentRequired = false;
        String indent = formatter.getIndent();
        for (int i = scope.depth; i > 0; --i) {
            builder.append(indent);
        }
    }

    private void newLine() {
        builder.append('\n');
        indentRequired = true;
    }

    public SqlBuilder definition(String tableAlias, ColumnDefinition definition) {
        return definition(tableAlias, definition, null);
    }

    public SqlBuilder definition(String tableAlias, ColumnDefinition definition, Function<Integer, String> asBlock) {
        if (tableAlias == null || tableAlias.isEmpty()) {
            return definition(definition);
        }
        preAppend();
        if (definition instanceof SingleColumn) {
            builder.append(tableAlias).append('.').append(((SingleColumn)definition).getName());
            if (asBlock != null) {
                builder.append(" ").append(asBlock.apply(0));
            }
        } else {
            int size = definition.size();
            for (int i = 0; i < size; i++) {
                if (i != 0) {
                    builder.append(", ");
                }
                builder.append(tableAlias).append('.').append(definition.name(i));
                if (asBlock != null) {
                    builder.append(" ").append(asBlock.apply(i));
                }
            }
        }
        return this;
    }

    public SqlBuilder definition(String tableAlias, ColumnDefinition definition, boolean applyEmbeddedScope) {
        if (applyEmbeddedScope && definition.isEmbedded()) {
            enter(ScopeType.TUPLE);
            definition(tableAlias, definition);
            leave();
        } else {
            definition(tableAlias, definition);
        }
        return this;
    }

    public SqlBuilder definition(ColumnDefinition definition) {
        preAppend();
        if (definition instanceof SingleColumn) {
            builder.append(((SingleColumn)definition).getName());
        } else {
            boolean addComma = false;
            for (String columnName : definition) {
                if (addComma) {
                    builder.append(", ");
                } else {
                    addComma = true;
                }
                builder.append(columnName);
            }
        }
        return this;
    }

    public SqlBuilder assignment(ImmutableProp prop, Object value) {
        ColumnDefinition definition = prop.getStorage(getAstContext().getSqlClient().getMetadataStrategy());
        preAppend();
        if (definition instanceof SingleColumn) {
            builder.append(((SingleColumn)definition).getName()).append(" = ");
            if (value != null) {
                variable(value);
            } else {
                nullVariable(prop.getReturnClass());
            }
        } else {
            ImmutableType type;
            if (prop.isEmbedded(EmbeddedLevel.SCALAR)) {
                type = prop.getTargetType();
            } else {
                type = prop.getTargetType().getIdProp().getTargetType();
            }
            List<Class<?>> subTypes = EmbeddableObjects.expandTypes(type);
            Object[] subValues = EmbeddableObjects.expand(type, value);
            int size = definition.size();
            for (int i = 0; i < size; i++) {
                if (i != 0) {
                    builder.append(", ");
                }
                builder.append(definition.name(i)).append(" = ");
                Object subValue = subValues[i];
                if (subValue != null) {
                    variable(subValue);
                } else {
                    nullSingeVariable(subTypes.get(i));
                }
            }
        }
        return this;
    }

    public SqlBuilder sql(String sql) {
        preAppend();
        builder.append(sql);
        return this;
    }

    public SqlBuilder from() {
        preAppend();
        space('?');
        preAppend();
        builder.append("from ");
        return this;
    }

    public SqlBuilder join(JoinType joinType) {
        preAppend();
        space('?');
        preAppend();
        builder.append(joinType.name().toLowerCase()).append(" join ");
        return this;
    }

    public SqlBuilder on() {
        space('?');
        preAppend();
        if (formatter.isPretty()) {
            builder.append(formatter.getIndent());
        }
        builder.append("on ");
        return this;
    }

    public <T> SqlBuilder variable(Class<T> type, T value) {
        if (value != null) {
            return variable(value);
        }
        return nullVariable(type);
    }

    public SqlBuilder variable(Object value) {
        validate();
        if (value instanceof TupleImplementor) {
            if (value instanceof Tuple2<?,?>) {
                Tuple2<?,?> tuple = (Tuple2<?,?>)value;
                this
                        .enter(ScopeType.TUPLE)
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                        .leave();
            } else if (value instanceof Tuple3<?,?,?>) {
                Tuple3<?,?,?> tuple = (Tuple3<?,?,?>)value;
                this
                        .enter(ScopeType.TUPLE)
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_3(), "tuple.get_3 cannot be null"))
                        .leave();
            } else if (value instanceof Tuple4<?,?,?,?>) {
                Tuple4<?,?,?,?> tuple = (Tuple4<?,?,?,?>)value;
                this
                        .enter(ScopeType.TUPLE)
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_3(), "tuple.get_3 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_4(), "tuple.get_4 cannot be null"))
                        .leave();
            } else if (value instanceof Tuple5<?,?,?,?,?>) {
                Tuple5<?,?,?,?,?> tuple = (Tuple5<?,?,?,?,?>)value;
                this
                        .enter(ScopeType.TUPLE)
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_3(), "tuple.get_3 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_4(), "tuple.get_4 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_5(), "tuple.get_5 cannot be null"))
                        .leave();
            } else if (value instanceof Tuple6<?,?,?,?,?,?>) {
                Tuple6<?,?,?,?,?,?> tuple = (Tuple6<?,?,?,?,?,?>)value;
                this
                        .enter(ScopeType.TUPLE)
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_3(), "tuple.get_3 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_4(), "tuple.get_4 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_5(), "tuple.get_5 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_6(), "tuple.get_6 cannot be null"))
                        .leave();
            } else if (value instanceof Tuple7<?,?,?,?,?,?,?>) {
                Tuple7<?,?,?,?,?,?,?> tuple = (Tuple7<?,?,?,?,?,?,?>)value;
                this
                        .enter(ScopeType.TUPLE)
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_3(), "tuple.get_3 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_4(), "tuple.get_4 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_5(), "tuple.get_5 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_6(), "tuple.get_6 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_7(), "tuple.get_7 cannot be null"))
                        .leave();
            } else if (value instanceof Tuple8<?,?,?,?,?,?,?,?>) {
                Tuple8<?,?,?,?,?,?,?,?> tuple = (Tuple8<?,?,?,?,?,?,?,?>)value;
                this
                        .enter(ScopeType.TUPLE)
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_3(), "tuple.get_3 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_4(), "tuple.get_4 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_5(), "tuple.get_5 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_6(), "tuple.get_6 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_7(), "tuple.get_7 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_8(), "tuple.get_8 cannot be null"))
                        .leave();
            } else if (value instanceof Tuple9<?,?,?,?,?,?,?,?,?>) {
                Tuple9<?,?,?,?,?,?,?,?,?> tuple = (Tuple9<?,?,?,?,?,?,?,?,?>)value;
                this
                        .enter(ScopeType.TUPLE)
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_3(), "tuple.get_3 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_4(), "tuple.get_4 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_5(), "tuple.get_5 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_6(), "tuple.get_6 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_7(), "tuple.get_7 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_8(), "tuple.get_8 cannot be null"))
                        .separator()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_9(), "tuple.get_9 cannot be null"))
                        .leave();
            }
        } else {
            nonTupleVariable(value);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    private SqlBuilder nonTupleVariable(Object value) {
        if (value instanceof ImmutableSpi) {
            ImmutableSpi spi = (ImmutableSpi)value;
            ImmutableType type = spi.__type();
            if (type.isEntity()) {
                nonTupleVariable(spi.__get(type.getIdProp().getId()));
            } else if (type.isEmbeddable()) {
                embeddedVariable(spi, null);
            } else {
                throw new IllegalArgumentException("Immutable variable must be entity or embeddable");
            }
        } else if (value instanceof DbNull) {
            preAppend();
            builder.append('?');
            variables.add(value);
            if (variableIndices != null) {
                variableIndices.add(builder.length());
            }
        } else {
            ScalarProvider<Object, Object> scalarProvider =
                    ctx.getSqlClient().getScalarProvider((Class<Object>) value.getClass());
            Object finalValue;
            if (scalarProvider != null) {
                try {
                    finalValue = scalarProvider.toSql(value);
                } catch (Exception ex) {
                    throw new ExecutionException(
                            "Cannot convert the jvm type \"" +
                                    value +
                                    "\" to the sql type \"" +
                                    scalarProvider.getSqlType() +
                                    "\"",
                            ex
                    );
                }
            } else {
                finalValue = value;
            }
            preAppend();
            builder.append('?');
            variables.add(finalValue);
            if (variableIndices != null) {
                variableIndices.add(builder.length());
            }
        }
        return this;
    }

    private void embeddedVariable(ImmutableSpi spi, EmbeddedPath parentPath) {
        enter(ScopeType.TUPLE);
        for (ImmutableProp prop : spi.__type().getProps().values()) {
            separator();
            EmbeddedPath path = new EmbeddedPath(parentPath, prop);
            if (!spi.__isLoaded(prop.getId())) {
                throw new IllegalArgumentException(
                        "Embedded object must loaded, the property path \"" +
                                path +
                                "\" is unloaded"
                );
            }
            Object value = spi.__get(prop.getId());
            if (value == null) {
                nullVariable(prop);
            } else if (value instanceof ImmutableSpi) {
                embeddedVariable((ImmutableSpi) value, path);
            } else {
                nonTupleVariable(value);
            }
        }
        leave();
    }

    public SqlBuilder nullVariable(ImmutableProp prop) {
        ImmutableType targetType = prop.getTargetType();
        if (targetType == null) {
            return nullVariable(prop.getElementClass());
        }
        return nullVariable(targetType.getIdProp().getElementClass());
    }

    public SqlBuilder nullVariable(Class<?> type) {
        ImmutableType immutableType = ImmutableType.tryGet(type);
        if (immutableType != null) {
            nullImmutableVariable(immutableType);
        } else {
            nullSingeVariable(type);
        }
        return this;
    }

    private void nullImmutableVariable(ImmutableType type) {
        if (type.isEntity()) {
            ImmutableProp idProp = type.getIdProp();
            if (idProp.isEmbedded(EmbeddedLevel.SCALAR)) {
                nullEmbeddedVariable(idProp.getTargetType());
            } else {
                nullSingeVariable(idProp.getElementClass());
            }
        } else if (type.isEmbeddable()) {
            nullEmbeddedVariable(type);
        } else {
            throw new IllegalArgumentException("Immutable variable must be entity or embeddable");
        }
    }

    private void nullEmbeddedVariable(ImmutableType type) {
        validate();
        enter(ScopeType.TUPLE);
        for (ImmutableProp prop : type.getProps().values()) {
            ImmutableType targetType = prop.getTargetType();
            if (targetType != null) {
                nullEmbeddedVariable(targetType);
            } else {
                nullSingeVariable(prop.getElementClass());
            }
        }
        leave();
    }

    @SuppressWarnings("unchecked")
    private void nullSingeVariable(Class<?> type) {
        validate();
        ScalarProvider<Object, Object> scalarProvider =
                ctx.getSqlClient().getScalarProvider((Class<Object>)type);
        Object finalValue;
        if (scalarProvider != null) {
            finalValue = new DbNull(scalarProvider.getSqlType());
        } else {
            finalValue = new DbNull(type);
        }
        preAppend();
        builder.append('?');
        variables.add(finalValue);
        if (variableIndices != null) {
            variableIndices.add(builder.length());
        }
    }

    public SqlBuilder createChildBuilder() {
        return new SqlBuilder(this);
    }

    public Tuple3<String, List<Object>, List<Integer>> build() {
        return build(null);
    }

    public Tuple3<String, List<Object>, List<Integer>> build(
            Function<
                    Tuple3<String, List<Object>, List<Integer>>,
                    Tuple3<String, List<Object>, List<Integer>>
            > transformer
    ) {
        if (scope != null) {
            throw new IllegalStateException("Internal bug: Did not leave all scopes");
        }
        validate();
        Tuple3<String, List<Object>, List<Integer>> result = new Tuple3<>(
                builder.toString(),
                variables,
                variableIndices
        );
        if (transformer != null) {
            result = transformer.apply(result);
        }
        SqlBuilder p = this.parent;
        if (p != null) {
            preAppend();
            p.builder.append(result.get_1());
            p.variables.addAll(result.get_2());
            if (p.variableIndices != null) {
                p.variableIndices.addAll(result.get_3());
            }
            while (p != null) {
                --p.childBuilderCount;
                p = p.parent;
            }
        }
        terminated = true;
        return result;
    }

    private void validate() {
        if (childBuilderCount != 0) {
            throw new IllegalStateException(
                    "Internal bug: Cannot change sqlbuilder because there are some child builders"
            );
        }
        if (terminated) {
            throw new IllegalStateException(
                    "Internal bug: Current build has been terminated"
            );
        }
    }

    private static class EmbeddedPath {

        final EmbeddedPath parent;

        final ImmutableProp prop;

        EmbeddedPath(EmbeddedPath parent, ImmutableProp prop) {
            this.parent = parent;
            this.prop = prop;
        }

        @Override
        public String toString() {
            if (parent == null) {
                return prop.getName();
            }
            return parent.toString() + '.' + prop.getName();
        }
    }

    public enum ScopeType {
        BLANK(null, null, null),
        SELECT("select?", ",?", null),
        SELECT_DISTINCT("select distinct?", ",?", null),
        SET("?set?", ",?", null),
        WHERE("?where?", "?and?", null),
        ORDER_BY("?order by?", ",?", null),
        GROUP_BY("?group by?", ",?", null),
        HAVING("?having?", ",?", null),
        SUB_QUERY("(\n", null, "\n)"),
        LIST("(\n", ",?", "\n)"),
        TUPLE("(", ", ", ")"),
        AND(null, "?and?", null, false),
        OR(null, "?or?", null, false),
        VALUES("?values\n", ",?", null);

        final Part prefix;

        final Part separator;

        final Part suffix;

        final boolean isSeparatorIndent;

        ScopeType(String prefix, String separator, String suffix) {
            this(prefix, separator, suffix, true);
        }

        ScopeType(String prefix, String separator, String suffix, boolean isSeparatorIndent) {
            this.prefix = partOf(prefix);
            this.separator = partOf(separator);
            this.suffix = partOf(suffix);
            this.isSeparatorIndent = isSeparatorIndent;
        }

        static class Part {

            final char before;
            final String value;
            final char after;

            Part(char before, String value, char after) {
                this.before = before;
                this.value = value;
                this.after = after;
            }
        }

        static Part partOf(String value) {
            if (value == null) {
                return null;
            }
            char before = spaceChar(value.charAt(0));
            char after = value.length() > 1 ? spaceChar(value.charAt(value.length() - 1)) : 0;
            return new Part(
                    before,
                    value.substring(before == '\0' ? 0 : 1, value.length() - (after == '\0' ? 0 : 1)),
                    after
            );
        }

        private static char spaceChar(char c) {
            return c == ' ' || c == '\n' || c == '?' ? c : '\0';
        }
    }

    private static class Scope {

        final Scope parent;

        final ScopeType type;

        final boolean ignored;

        final ScopeType.Part separator;

        int depth;

        boolean dirty;

        int listSeparatorCount;

        private Scope(
                Scope parent,
                ScopeType type,
                boolean ignored,
                String separator
        ) {
            this.parent = parent;
            this.type = type;
            this.ignored = ignored;
            this.depth = ignored ? parent.depth : (parent != null ? parent.depth + 1: 1);
            this.separator = separator != null ? ScopeType.partOf(separator) : type.separator;
        }

        void setDirty() {
            for (Scope scope = this; scope != null; scope = scope.parent) {
                if (scope.dirty) {
                    break;
                }
                scope.dirty = true;
            }
        }
    }
}