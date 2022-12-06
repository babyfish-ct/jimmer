package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
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

    private final StringBuilder builder = new StringBuilder();

    private final List<Object> variables = new ArrayList<>();

    private int childBuilderCount;

    private int tupleDepth = 0;

    private boolean terminated;

    public SqlBuilder(AstContext ctx) {
        this.ctx = ctx;
        this.parent = null;
    }

    private SqlBuilder(SqlBuilder parent) {
        this.ctx = parent.ctx;
        this.parent = parent;
        parent.childBuilderCount++;
    }

    public AstContext getAstContext() {
        return ctx;
    }

    public SqlBuilder sql(String tableAlias, ColumnDefinition definition) {
        if (tableAlias == null || tableAlias.isEmpty()) {
            return sql(definition);
        }
        if (definition instanceof SingleColumn) {
            builder.append(tableAlias).append('.').append(((SingleColumn)definition).getName());
        } else {
            boolean addComma = false;
            for (String columnName : definition) {
                if (addComma) {
                    builder.append(", ");
                } else {
                    addComma = true;
                }
                builder.append(tableAlias).append('.').append(columnName);
            }
        }
        return this;
    }

    public SqlBuilder sql(String tableAlias, ColumnDefinition definition, boolean applyEmbeddedScope) {
        if (applyEmbeddedScope && definition.isEmbedded()) {
            enterTuple();
            sql(tableAlias, definition);
            leaveTuple();
        } else {
            sql(tableAlias, definition);
        }
        return this;
    }

    public SqlBuilder sql(ColumnDefinition definition) {
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
        ColumnDefinition definition = prop.getStorage();
        if (definition instanceof SingleColumn) {
            builder.append(((SingleColumn)definition).getName()).append(" = ");
            if (value != null) {
                variable(value);
            } else {
                nullSingeVariable(prop.getElementClass());
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
        builder.append(sql);
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
                        .enterTuple()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                        .leaveTuple();
            } else if (value instanceof Tuple3<?,?,?>) {
                Tuple3<?,?,?> tuple = (Tuple3<?,?,?>)value;
                this
                        .enterTuple()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_3(), "tuple.get_3 cannot be null"))
                        .leaveTuple();
            } else if (value instanceof Tuple4<?,?,?,?>) {
                Tuple4<?,?,?,?> tuple = (Tuple4<?,?,?,?>)value;
                this
                        .enterTuple()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_3(), "tuple.get_3 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_4(), "tuple.get_4 cannot be null"))
                        .leaveTuple();
            } else if (value instanceof Tuple5<?,?,?,?,?>) {
                Tuple5<?,?,?,?,?> tuple = (Tuple5<?,?,?,?,?>)value;
                this
                        .enterTuple()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_3(), "tuple.get_3 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_4(), "tuple.get_4 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_5(), "tuple.get_5 cannot be null"))
                        .leaveTuple();
            } else if (value instanceof Tuple6<?,?,?,?,?,?>) {
                Tuple6<?,?,?,?,?,?> tuple = (Tuple6<?,?,?,?,?,?>)value;
                this
                        .enterTuple()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_3(), "tuple.get_3 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_4(), "tuple.get_4 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_5(), "tuple.get_5 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_6(), "tuple.get_6 cannot be null"))
                        .leaveTuple();
            } else if (value instanceof Tuple7<?,?,?,?,?,?,?>) {
                Tuple7<?,?,?,?,?,?,?> tuple = (Tuple7<?,?,?,?,?,?,?>)value;
                this
                        .enterTuple()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_3(), "tuple.get_3 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_4(), "tuple.get_4 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_5(), "tuple.get_5 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_6(), "tuple.get_6 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_7(), "tuple.get_7 cannot be null"))
                        .leaveTuple();
            } else if (value instanceof Tuple8<?,?,?,?,?,?,?,?>) {
                Tuple8<?,?,?,?,?,?,?,?> tuple = (Tuple8<?,?,?,?,?,?,?,?>)value;
                this
                        .enterTuple()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_3(), "tuple.get_3 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_4(), "tuple.get_4 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_5(), "tuple.get_5 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_6(), "tuple.get_6 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_7(), "tuple.get_7 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_8(), "tuple.get_8 cannot be null"))
                        .leaveTuple();
            } else if (value instanceof Tuple9<?,?,?,?,?,?,?,?,?>) {
                Tuple9<?,?,?,?,?,?,?,?,?> tuple = (Tuple9<?,?,?,?,?,?,?,?,?>)value;
                this
                        .enterTuple()
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_3(), "tuple.get_3 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_4(), "tuple.get_4 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_5(), "tuple.get_5 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_6(), "tuple.get_6 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_7(), "tuple.get_7 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_8(), "tuple.get_8 cannot be null"))
                        .sql(", ")
                        .nonTupleVariable(Objects.requireNonNull(tuple.get_9(), "tuple.get_9 cannot be null"))
                        .leaveTuple();
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
            throw new ExecutionException(
                    "Cannot add variable whose type is " + DbNull.class.getName()
            );
        } else {
            ScalarProvider<Object, Object> scalarProvider =
                    ctx.getSqlClient().getScalarProvider((Class<Object>) value.getClass());
            Object finalValue;
            if (scalarProvider != null) {
                finalValue = scalarProvider.toSql(value);
            } else {
                finalValue = value;
            }
            builder.append('?');
            variables.add(finalValue);
        }
        return this;
    }

    private void embeddedVariable(ImmutableSpi spi, EmbeddedPath parentPath) {
        enterTuple();
        boolean addComma = false;
        for (ImmutableProp prop : spi.__type().getProps().values()) {
            if (addComma) {
                builder.append(", ");
            } else {
                addComma = true;
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
                nullVariable(prop);
            } else if (value instanceof ImmutableSpi) {
                embeddedVariable((ImmutableSpi) value, path);
            } else {
                nonTupleVariable(value);
            }
        }
        leaveTuple();
    }

    public SqlBuilder nullVariable(ImmutableProp prop) {
        validate();
        ImmutableType targetType = prop.getTargetType();
        if (targetType == null) {
            return nullVariable(prop.getElementClass());
        }
        return nullVariable(targetType.getIdProp().getElementClass());
    }

    @SuppressWarnings("unchecked")
    public SqlBuilder nullVariable(Class<?> type) {
        validate();
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
        enterTuple();
        for (ImmutableProp prop : type.getProps().values()) {
            ImmutableType targetType = prop.getTargetType();
            if (targetType != null) {
                nullEmbeddedVariable(targetType);
            } else {
                nullSingeVariable(prop.getElementClass());
            }
        }
        leaveTuple();
    }

    @SuppressWarnings("unchecked")
    private void nullSingeVariable(Class<?> type) {
        ScalarProvider<Object, Object> scalarProvider =
                ctx.getSqlClient().getScalarProvider((Class<Object>)type);
        Object finalValue;
        if (scalarProvider != null) {
            finalValue = new DbNull(scalarProvider.getSqlType());
        } else {
            finalValue = new DbNull(type);
        }
        builder.append('?');
        variables.add(finalValue);
    }

    public SqlBuilder enterTuple() {
        if (this.tupleDepth++ == 0) {
            builder.append('(');
        }
        return this;
    }

    public SqlBuilder leaveTuple() {
        if (--this.tupleDepth == 0) {
            builder.append(')');
        }
        return this;
    }

    public SqlBuilder createChildBuilder() {
        return new SqlBuilder(this);
    }

    public Tuple2<String, List<Object>> build() {
        return build(null);
    }

    public Tuple2<String, List<Object>> build(
            Function<Tuple2<String, List<Object>>, Tuple2<String, List<Object>>> transformer
    ) {
        validate();
        Tuple2<String, List<Object>> result = new Tuple2<>(builder.toString(), variables);
        if (transformer != null) {
            result = transformer.apply(result);
        }
        SqlBuilder p = this.parent;
        if (p != null) {
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
}