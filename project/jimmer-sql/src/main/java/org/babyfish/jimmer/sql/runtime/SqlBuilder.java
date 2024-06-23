package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.TupleImplementor;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.util.ArrayUtils;
import org.babyfish.jimmer.sql.ast.tuple.*;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.meta.SingleColumn;

import java.util.*;
import java.util.function.Function;

public class SqlBuilder extends AbstractSqlBuilder<SqlBuilder> {

    private static final Map<Class<?>, Converter<?, ?>> ARRAY_CONVERTER_MAP;

    private final AstContext ctx;

    private final ScopeManager scopeManager;

    private final Ref<Scope> rollbackTo;

    private final SqlBuilder parent;

    private final boolean nonNullVariableOnly;

    private final SqlFormatter formatter;

    private final List<Object> variables = new ArrayList<>();

    private final List<Integer> variablePositions;

    private int childBuilderCount;

    private boolean terminated;

    private boolean aborted;

    public SqlBuilder(AstContext ctx) {
        this.ctx = ctx;
        this.scopeManager = new ScopeManager();
        this.parent = null;
        this.rollbackTo = null;
        nonNullVariableOnly = false;
        this.formatter = ctx.getSqlClient().getSqlFormatter();
        if (ctx.getSqlClient().getSqlFormatter().isPretty()) {
            this.variablePositions = new ArrayList<>();
        } else {
            this.variablePositions = null;
        }
    }

    private SqlBuilder(SqlBuilder parent, boolean isAbortingSupported, boolean nonNullVariableOnly) {
        if (nonNullVariableOnly && !isAbortingSupported) {
            throw new IllegalArgumentException(
                    "`isAbortingSupported` must be true when `nonNullVariableOnly` is true"
            );
        }
        this.ctx = parent.ctx;
        this.scopeManager = parent.scopeManager;
        this.parent = parent;
        if (isAbortingSupported) {
            this.rollbackTo = Ref.of(scopeManager.cloneScope());
        } else {
            this.rollbackTo = null;
        }
        this.nonNullVariableOnly = nonNullVariableOnly;
        this.formatter = ctx.getSqlClient().getSqlFormatter();
        if (ctx.getSqlClient().getSqlFormatter().isPretty()) {
            this.variablePositions = new ArrayList<>();
        } else {
            this.variablePositions = null;
        }
        for (SqlBuilder p = parent; p != null; p = p.parent) {
            p.childBuilderCount++;
        }
    }

    @Override
    public JSqlClientImplementor sqlClient() {
        return ctx.getSqlClient();
    }

    @Override
    protected final SqlFormatter formatter() {
        return formatter;
    }

    @Override
    protected final ScopeManager scopeManager() {
        return scopeManager;
    }

    public AstContext getAstContext() {
        return ctx;
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

    @Override
    public SqlBuilder rawVariable(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("\"value\" cannot be null");
        }
        preAppend();
        builder.append('?');
        addVariable(value);
        if (variablePositions != null) {
            variablePositions.add(builder.length());
        }
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
        if (value instanceof DbLiteral) {
            preAppend();
            ((DbLiteral)value).render(builder, getAstContext().getSqlClient());
            addVariable(value);
            if (variablePositions != null) {
                variablePositions.add(builder.length());
            }
            return this;
        }
        ScalarProvider<Object, Object> scalarProvider =
            ctx.getSqlClient().getScalarProvider((Class<Object>) value.getClass());
        if (scalarProvider != null) {
            try {
                value = scalarProvider.toSql(value);
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
        }
        if (value instanceof ImmutableSpi) {
            ImmutableSpi spi = (ImmutableSpi)value;
            ImmutableType type = spi.__type();
            if (type.isEntity()) {
                nonTupleVariable(spi.__get(type.getIdProp().getId()));
            } else if (type.isEmbeddable()) {
                embeddedVariable(spi);
            } else {
                throw new IllegalArgumentException("Immutable variable must be entity or embeddable");
            }
        } else {
            Converter<?, ?> arrayConverter = ARRAY_CONVERTER_MAP.get(value.getClass());
            if (arrayConverter != null) {
                value = ((Converter<Object, Object>)arrayConverter).convert(value);
            }
            preAppend();
            builder.append('?');
            addVariable(value);
            if (variablePositions != null) {
                variablePositions.add(builder.length());
            }
        }
        return this;
    }

    private void embeddedVariable(ImmutableSpi spi) {
        enter(ScopeType.TUPLE);
        embeddedVariableImpl(spi, null);
        leave();
    }

    private void embeddedVariableImpl(ImmutableSpi spi, EmbeddedPath parentPath) {
        for (ImmutableProp prop : spi.__type().getProps().values()) {
            if (prop.isFormula()) {
                continue;
            }
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
                separator();
                nullVariable(prop);
            } else if (value instanceof ImmutableSpi) {
                embeddedVariableImpl((ImmutableSpi) value, path);
            } else {
                separator();
                nonTupleVariable(value);
            }
        }
    }

    public SqlBuilder nullVariable(ImmutableProp prop) {
        ImmutableType targetType = prop.getTargetType();
        if (targetType == null) {
            return nullVariable(prop.getElementClass());
        }
        if (targetType.isEmbeddable()) {
            return nullEmbeddedVariable(targetType);
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

    private SqlBuilder nullImmutableVariable(ImmutableType type) {
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
        return this;
    }

    private SqlBuilder nullEmbeddedVariable(ImmutableType type) {
        validate();
        enter(ScopeType.TUPLE);
        nullEmbeddedVariableImpl(type);
        leave();
        return this;
    }

    private void nullEmbeddedVariableImpl(ImmutableType type) {
        for (ImmutableProp prop : type.getProps().values()) {
            if (prop.isFormula()) {
                continue;
            }
            ImmutableType targetType = prop.getTargetType();
            if (targetType != null) {
                nullEmbeddedVariableImpl(targetType);
            } else {
                separator();
                nullSingeVariable(prop.getElementClass());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void nullSingeVariable(Class<?> type) {
        validate();
        ScalarProvider<Object, Object> scalarProvider =
                ctx.getSqlClient().getScalarProvider((Class<Object>)type);
        Object finalValue;
        if (scalarProvider != null) {
            finalValue = new DbLiteral.DbNull(scalarProvider.getSqlType());
        } else {
            finalValue = new DbLiteral.DbNull(type);
        }
        preAppend();
        builder.append('?');
        addVariable(finalValue);
        if (variablePositions != null) {
            variablePositions.add(builder.length());
        }
    }

    public SqlBuilder createChildBuilder() {
        return new SqlBuilder(this, false, false);
    }

    public SqlBuilder createChildBuilder(boolean isAbortingSupported, boolean nonNullVariableOnly) {
        return new SqlBuilder(this, isAbortingSupported, nonNullVariableOnly);
    }

    public SqlBuilder abort() {
        if (rollbackTo == null) {
            throw new IllegalStateException("The current sql builder is not allowed to be aborted");
        }
        aborted = true;
        return this;
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
        if (parent == null && scopeManager.current != null) {
            throw new IllegalStateException("Internal bug: Did not leave all scopes");
        }
        validate();
        if (aborted) {
            scopeManager.current = rollbackTo.getValue();
            SqlBuilder p = parent;
            while (p != null) {
                --p.childBuilderCount;
                p = p.parent;
            }
            terminated = true;
            return new Tuple3<>("", Collections.emptyList(), Collections.emptyList());
        }
        Tuple3<String, List<Object>, List<Integer>> result = new Tuple3<>(
                builder.toString(),
                variables,
                variablePositions
        );
        if (transformer != null) {
            result = transformer.apply(result);
        }
        SqlBuilder p = this.parent;
        if (p != null) {
            preAppend();
            if (p.variablePositions != null) {
                int base = p.builder.length();
                for (Integer position : result.get_3()) {
                    p.variablePositions.add(base + position);
                }
            }
            p.builder.append(result.get_1());
            p.variables.addAll(result.get_2());
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

    private void addVariable(Object value) {
        if (nonNullVariableOnly && value instanceof DbLiteral.DbNull) {
            throw new NullVariableException();
        }
        variables.add(value);
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

    private interface Converter<S, T> {
        T convert(S value);
    }

    public class NullVariableException extends RuntimeException {}

    static {
        Map<Class<?>, Converter<?, ?>> map = new HashMap<>();
        map.put(boolean[].class, new Converter<boolean[], Boolean[]>() {
            @Override
            public Boolean[] convert(boolean[] arr) {
                return ArrayUtils.toObject(arr);
            }
        });
        map.put(char[].class, new Converter<char[], Character[]>() {
            @Override
            public Character[] convert(char[] arr) {
                return ArrayUtils.toObject(arr);
            }
        });
        map.put(short[].class, new Converter<short[], Short[]>() {
            @Override
            public Short[] convert(short[] arr) {
                return ArrayUtils.toObject(arr);
            }
        });
        map.put(int[].class, new Converter<int[], Integer[]>() {
            @Override
            public Integer[] convert(int[] arr) {
                return ArrayUtils.toObject(arr);
            }
        });
        map.put(long[].class, new Converter<long[], Long[]>() {
            @Override
            public Long[] convert(long[] arr) {
                return ArrayUtils.toObject(arr);
            }
        });
        map.put(float[].class, new Converter<float[], Float[]>() {
            @Override
            public Float[] convert(float[] arr) {
                return ArrayUtils.toObject(arr);
            }
        });
        map.put(double[].class, new Converter<double[], Double[]>() {
            @Override
            public Double[] convert(double[] arr) {
                return ArrayUtils.toObject(arr);
            }
        });
        ARRAY_CONVERTER_MAP = map;
    }
}