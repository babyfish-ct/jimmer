package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.*;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.ast.impl.render.BatchSqlBuilder;
import org.babyfish.jimmer.sql.ast.tuple.*;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiFunction;

class Tuples {

    public static TupleImplementor valueOf(Object[] arr) {
        switch (arr.length) {
            case 0:
            case 1:
                throw new IllegalArgumentException("The `arr.length` must be greater than or equal to 2");
            case 2:
                return new Tuple2<>(arr[0], arr[1]);
            case 3:
                return new Tuple3<>(arr[0], arr[1], arr[2]);
            case 4:
                return new Tuple4<>(arr[0], arr[1], arr[2], arr[3]);
            case 5:
                return new Tuple5<>(arr[0], arr[1], arr[2], arr[3], arr[4]);
            case 6:
                return new Tuple6<>(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5]);
            case 7:
                return new Tuple7<>(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5], arr[6]);
            case 8:
                return new Tuple8<>(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5], arr[6], arr[7]);
            case 9:
                return new Tuple9<>(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5], arr[6], arr[7], arr[8]);
            default:
                return new LongTuple(arr);
        }
    }

    @SuppressWarnings("unchecked")
    public static Expression<Object> expressionOf(Expression<?>[] arr) {
        switch (arr.length) {
            case 0:
            case 1:
                throw new IllegalArgumentException("The `arr.length` must be greater than or equal to 2");
            case 2:
                return (Expression<Object>) (Expression<?>) Expression.tuple(arr[0], arr[1]);
            case 3:
                return (Expression<Object>) (Expression<?>) Expression.tuple(arr[0], arr[1], arr[2]);
            case 4:
                return (Expression<Object>) (Expression<?>) Expression.tuple(arr[0], arr[1], arr[2], arr[3]);
            case 5:
                return (Expression<Object>) (Expression<?>) Expression.tuple(arr[0], arr[1], arr[2], arr[3], arr[4]);
            case 6:
                return (Expression<Object>) (Expression<?>) Expression.tuple(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5]);
            case 7:
                return (Expression<Object>) (Expression<?>) Expression.tuple(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5], arr[6]);
            case 8:
                return (Expression<Object>) (Expression<?>) Expression.tuple(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5], arr[6], arr[7]);
            case 9:
                return (Expression<Object>) (Expression<?>) Expression.tuple(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5], arr[6], arr[7], arr[8]);
            default:
                return (Expression<Object>) (Expression<?>) new LongTupleExpression(arr);
        }
    }

    private static class LongTuple implements TupleImplementor {

        private final Object[] arr;

        private LongTuple(Object[] arr) {
            this.arr = arr;
        }

        @Override
        public int size() {
            return arr.length;
        }

        @Override
        public Object get(int index) {
            return arr[index];
        }

        @Override
        public TupleImplementor convert(BiFunction<Object, Integer, Object> block) {
            Object[] newArr = null;
            for (int i = arr.length - 1; i >= 0; --i) {
                Object oldValue = arr[i];
                Object newValue = block.apply(oldValue, i);
                if (Objects.equals(oldValue, newValue)) {
                    continue;
                }
                if (newArr == null) {
                    newArr = arr.clone();
                }
                newArr[i] = newValue;
            }
            if (newArr == null) {
                return this;
            }
            return new LongTuple(newArr);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(arr);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof LongTuple)) {
                return false;
            }
            LongTuple other = (LongTuple) obj;
            return Arrays.equals(arr, other.arr);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            int size = arr.length;
            builder.append("Tuple").append(size).append('(');
            for (int i = 0; i < size; i++) {
                if (i != 0) {
                    builder.append(", ");
                }
                builder.append('_').append(i + 1).append('=').append(arr[i]);
            }
            builder.append(')');
            return builder.toString();
        }
    }

    private static class LongTupleExpression
            extends AbstractExpression<TupleImplementor>
            implements TupleExpressionImplementor<TupleImplementor> {

        private final Expression<?>[] arr;

        public LongTupleExpression(Expression<?>[] arr) {
            this.arr = arr;
        }

        @Override
        protected boolean determineHasVirtualPredicate() {
            for (Selection<?> selection : arr) {
                if (hasVirtualPredicate(selection)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected Ast onResolveVirtualPredicate(AstContext ctx) {
            Expression<?>[] newArr = null;
            int size = arr.length;
            for (int i = size - 1; i >= 0; --i) {
                Expression<?> oldExpr = arr[i];
                Expression<?> newExpr = ctx.resolveVirtualPredicate(oldExpr);
                if (oldExpr == newExpr) {
                    continue;
                }
                if (newArr == null) {
                    newArr = arr.clone();
                }
                newArr[i] = newExpr;
            }
            if (newArr == null) {
                return this;
            }
            return new LongTupleExpression(newArr);
        }

        @Override
        public void accept(@NotNull AstVisitor visitor) {
            for (Selection<?> selection : arr) {
                ((Ast)selection).accept(visitor);
            }
        }

        @Override
        public void renderTo(@NotNull AbstractSqlBuilder<?> builder) {
            renderTo(builder, false);
        }

        @Override
        public void renderTo(@NotNull AbstractSqlBuilder<?> builder, boolean ignoreBrackets) {
            if (!ignoreBrackets) {
                builder.enter(SqlBuilder.ScopeType.TUPLE);
            }
            boolean addComma = false;
            for (Selection<?> selection : arr) {
                if (addComma) {
                    builder.sql(", ");
                } else {
                    addComma = true;
                }
                renderChild((Ast) selection, builder);
            }
            if (!ignoreBrackets) {
                builder.leave();
            }
        }

        @Override
        public Class<TupleImplementor> getType() {
            return TupleImplementor.class;
        }

        @Override
        public int precedence() {
            return 0;
        }

        @Override
        public int size() {
            return arr.length;
        }

        @Override
        public Selection<?> get(int index) {
            return arr[index];
        }
    }
}
