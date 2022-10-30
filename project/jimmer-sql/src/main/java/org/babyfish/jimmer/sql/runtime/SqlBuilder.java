package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.impl.AstContext;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.tuple.*;

import java.util.*;
import java.util.function.Function;

public class SqlBuilder {

    private AstContext ctx;

    private SqlBuilder parent;

    private int childBuilderCount;

    private StringBuilder builder = new StringBuilder();

    private List<Object> variables = new ArrayList<>();

    private boolean terminated;

    public SqlBuilder(AstContext ctx) {
        this.ctx = ctx;
    }

    private SqlBuilder(SqlBuilder parent) {
        this.ctx = parent.ctx;
        this.parent = parent;
        parent.childBuilderCount++;
    }

    public AstContext getAstContext() {
        return ctx;
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
        if (value instanceof Tuple2<?,?>) {
            Tuple2<?,?> tuple = (Tuple2<?,?>)value;
            this
                    .sql("(")
                    .singleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                    .sql(")");
        } else if (value instanceof Tuple3<?,?,?>) {
            Tuple3<?,?,?> tuple = (Tuple3<?,?,?>)value;
            this
                    .sql("(")
                    .singleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_3(), "tuple.get_3 cannot be null"))
                    .sql(")");
        } else if (value instanceof Tuple4<?,?,?,?>) {
            Tuple4<?,?,?,?> tuple = (Tuple4<?,?,?,?>)value;
            this
                    .sql("(")
                    .singleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_3(), "tuple.get_3 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_4(), "tuple.get_4 cannot be null"))
                    .sql(")");
        } else if (value instanceof Tuple5<?,?,?,?,?>) {
            Tuple5<?,?,?,?,?> tuple = (Tuple5<?,?,?,?,?>)value;
            this
                    .sql("(")
                    .singleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_3(), "tuple.get_3 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_4(), "tuple.get_4 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_5(), "tuple.get_5 cannot be null"))
                    .sql(")");
        } else if (value instanceof Tuple6<?,?,?,?,?,?>) {
            Tuple6<?,?,?,?,?,?> tuple = (Tuple6<?,?,?,?,?,?>)value;
            this
                    .sql("(")
                    .singleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_3(), "tuple.get_3 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_4(), "tuple.get_4 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_5(), "tuple.get_5 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_6(), "tuple.get_6 cannot be null"))
                    .sql(")");
        } else if (value instanceof Tuple7<?,?,?,?,?,?,?>) {
            Tuple7<?,?,?,?,?,?,?> tuple = (Tuple7<?,?,?,?,?,?,?>)value;
            this
                    .sql("(")
                    .singleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_3(), "tuple.get_3 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_4(), "tuple.get_4 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_5(), "tuple.get_5 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_6(), "tuple.get_6 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_7(), "tuple.get_7 cannot be null"))
                    .sql(")");
        } else if (value instanceof Tuple8<?,?,?,?,?,?,?,?>) {
            Tuple8<?,?,?,?,?,?,?,?> tuple = (Tuple8<?,?,?,?,?,?,?,?>)value;
            this
                    .sql("(")
                    .singleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_3(), "tuple.get_3 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_4(), "tuple.get_4 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_5(), "tuple.get_5 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_6(), "tuple.get_6 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_7(), "tuple.get_7 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_8(), "tuple.get_8 cannot be null"))
                    .sql(")");
        } else if (value instanceof Tuple9<?,?,?,?,?,?,?,?,?>) {
            Tuple9<?,?,?,?,?,?,?,?,?> tuple = (Tuple9<?,?,?,?,?,?,?,?,?>)value;
            this
                    .sql("(")
                    .singleVariable(Objects.requireNonNull(tuple.get_1(), "tuple.get_1 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_2(), "tuple.get_2 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_3(), "tuple.get_3 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_4(), "tuple.get_4 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_5(), "tuple.get_5 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_6(), "tuple.get_6 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_7(), "tuple.get_7 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_8(), "tuple.get_8 cannot be null"))
                    .sql(", ")
                    .singleVariable(Objects.requireNonNull(tuple.get_9(), "tuple.get_9 cannot be null"))
                    .sql(")");
        } else {
            singleVariable(value);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    private SqlBuilder singleVariable(Object value) {
        if (value instanceof DbNull) {
            throw new ExecutionException(
                    "Cannot add variable whose type is " + DbNull.class.getName()
            );
        }
        ScalarProvider<Object, Object> scalarProvider =
                ctx.getSqlClient().getScalarProvider((Class<Object>)value.getClass());
        Object finalValue;
        if (scalarProvider != null) {
            finalValue = scalarProvider.toSql(value);
        } else {
            finalValue = value;
        }
        builder.append('?');
        variables.add(finalValue);
        return this;
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
}