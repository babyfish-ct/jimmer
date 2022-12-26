package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.ComparableExpression;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.StringExpression;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SimpleCaseBuilder<C, T> {

    private Class<T> type;

    private Expression<?> expression;

    private List<Tuple2<Expression<?>, Expression<T>>> whens = new ArrayList<>();

    SimpleCaseBuilder(Class<T> type, Expression<?> expression) {
        this.type = type;
        this.expression = expression;
    }

    public SimpleCaseBuilder<C, T> when(C cond, T then) {
        return when(Literals.any(cond), Literals.any(then));
    }

    public SimpleCaseBuilder<C, T> when(Expression<C> cond, T then) {
        return when(cond, Literals.any(then));
    }

    public SimpleCaseBuilder<C, T> when(C cond, Expression<T> then) {
        return when(Literals.any(cond), then);
    }

    public SimpleCaseBuilder<C, T> when(Expression<C> cond, Expression<T> then) {
        whens.add(new Tuple2<>(cond, then));
        return this;
    }

    public Expression<T> otherwise(T otherwise) {
        return otherwise(Literals.any(otherwise));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Expression<T> otherwise(Expression<T> otherwise) {
        List<Tuple2<Expression<?>, Expression<T>>> whens = new ArrayList<>(this.whens);
        if (String.class.isAssignableFrom(type)) {
            return (Expression<T>) new StrExpr(
                    expression,
                    (List<Tuple2<Expression<?>, Expression<String>>>)(List<?>)whens,
                    (Expression<String>)otherwise
            );
        }
        if (type.isPrimitive() || Number.class.isAssignableFrom(type)) {
            return (Expression<T>) new NumExpr(
                    type,
                    expression,
                    (List<?>)whens,
                    otherwise
            );
        }
        if (Comparable.class.isAssignableFrom(type)) {
            return (Expression<T>) new CmpExpr(
                    type,
                    expression,
                    whens,
                    otherwise
            );
        }
        return new AnyExpr<>(type, expression, whens, otherwise);
    }
    
    public static class Str<C> extends SimpleCaseBuilder<C, String> {

        Str(Expression<?> expression) {
            super(String.class, expression);
        }

        @Override
        public Str<C> when(C cond, String then) {
            return (Str<C>) super.when(cond, then);
        }

        @Override
        public Str<C> when(Expression<C> cond, String then) {
            return (Str<C>) super.when(cond, then);
        }

        @Override
        public Str<C> when(C cond, Expression<String> then) {
            return (Str<C>) super.when(cond, then);
        }

        @Override
        public Str<C> when(Expression<C> cond, Expression<String> then) {
            return (Str<C>) super.when(cond, then);
        }

        @Override
        public StringExpression otherwise(String otherwise) {
            return (StringExpression) super.otherwise(otherwise);
        }

        @Override
        public StringExpression otherwise(Expression<String> otherwise) {
            return (StringExpression) super.otherwise(otherwise);
        }
    }
    
    public static class Num<C, N extends Number & Comparable<N>> extends SimpleCaseBuilder<C, N> {

        Num(Class<N> type, Expression<?> expression) {
            super(type, expression);
        }

        @Override
        public Num<C, N> when(C cond, N then) {
            return (Num<C, N>) super.when(cond, then);
        }

        @Override
        public Num<C, N> when(Expression<C> cond, N then) {
            return (Num<C, N>)super.when(cond, then);
        }

        @Override
        public Num<C, N> when(C cond, Expression<N> then) {
            return (Num<C, N>) super.when(cond, then);
        }

        @Override
        public Num<C, N> when(Expression<C> cond, Expression<N> then) {
            return (Num<C, N>)super.when(cond, then);
        }

        @Override
        public NumericExpression<N> otherwise(N otherwise) {
            return (NumericExpression<N>) super.otherwise(otherwise);
        }

        @Override
        public NumericExpression<N> otherwise(Expression<N> otherwise) {
            return (NumericExpression<N>) super.otherwise(otherwise);
        }
    }
    
    public static class Cmp<C, T extends Comparable<?>> extends SimpleCaseBuilder<C, T> {

        Cmp(Class<T> type, Expression<?> expression) {
            super(type, expression);
        }

        @Override
        public Cmp<C, T> when(C cond, T then) {
            return (Cmp<C, T>) super.when(cond, then);
        }

        @Override
        public Cmp<C, T> when(Expression<C> cond, T then) {
            return (Cmp<C, T>) super.when(cond, then);
        }

        @Override
        public Cmp<C, T> when(C cond, Expression<T> then) {
            return (Cmp<C, T>) super.when(cond, then);
        }

        @Override
        public Cmp<C, T> when(Expression<C> cond, Expression<T> then) {
            return (Cmp<C, T>) super.when(cond, then);
        }

        @Override
        public ComparableExpression<T> otherwise(T otherwise) {
            return (ComparableExpression<T>) super.otherwise(otherwise);
        }

        @Override
        public ComparableExpression<T> otherwise(Expression<T> otherwise) {
            return (ComparableExpression<T>) super.otherwise(otherwise);
        }
    }

    private static class AnyExpr<T> extends AbstractExpression<T> {

        private final Class<T> type;

        private final Expression<?> expression;

        private final List<Tuple2<Expression<?>, Expression<T>>> whens;

        private final Expression<T> otherwise;

        AnyExpr(
                Class<T> type,
                Expression<?> expression,
                List<Tuple2<Expression<?>, Expression<T>>> whens,
                Expression<T> otherwise
        ) {
            this.type = type;
            this.expression = expression;
            this.whens = whens;
            this.otherwise = otherwise;
        }


        @Override
        public Class<T> getType() {
            return type;
        }

        @Override
        public int precedence() {
            return 0;
        }

        @Override
        public void accept(@NotNull AstVisitor visitor) {
            ((Ast) expression).accept(visitor);
            for (Tuple2<Expression<?>, Expression<T>> when : whens) {
                ((Ast) when.get_1()).accept(visitor);
                ((Ast) when.get_2()).accept(visitor);
            }
            ((Ast) otherwise).accept(visitor);
        }

        @Override
        public void renderTo(@NotNull SqlBuilder builder) {
            usingLowestPrecedence(() -> {
                builder.sql("case ");
                renderChild((Ast) expression, builder);
                for (Tuple2<Expression<?>, Expression<T>> when : whens) {
                    builder.sql(" when ");
                    renderChild((Ast) when.get_1(), builder);
                    builder.sql(" then ");
                    renderChild((Ast) when.get_2(), builder);
                }
                builder.sql(" else ");
                renderChild((Ast) otherwise, builder);
                builder.sql(" end");
            });
        }
    }
    
    private static class StrExpr extends AnyExpr<String> implements StringExpressionImplementor {
        
        StrExpr(
                Expression<?> expression, 
                List<Tuple2<Expression<?>, Expression<String>>> whens, 
                Expression<String> otherwise
        ) {
            super(String.class, expression, whens, otherwise);
        }
    }
    
    private static class NumExpr<N extends Number & Comparable<N>> extends AnyExpr<N> implements NumericExpressionImplementor<N> {

        NumExpr(
                Class<N> type, 
                Expression<?> expression, 
                List<Tuple2<Expression<?>, Expression<N>>> whens, 
                Expression<N> otherwise
        ) {
            super(type, expression, whens, otherwise);
        }
    }
    
    private static class CmpExpr<T extends Comparable<?>> extends AnyExpr<T> implements ComparableExpressionImplementor<T> {
        
        CmpExpr(
                Class<T> type, 
                Expression<?> expression, List<Tuple2<Expression<?>, Expression<T>>> whens, 
                Expression<T> otherwise
        ) {
            super(type, expression, whens, otherwise);
        }
    }
}
