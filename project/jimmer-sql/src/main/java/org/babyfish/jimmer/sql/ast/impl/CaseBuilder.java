package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.*;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CaseBuilder<T> {

    private Class<T> type;

    private List<Tuple2<Predicate, Expression<T>>> whens = new ArrayList<>();

    CaseBuilder(Class<T> type) {
        this.type = type;
    }

    public CaseBuilder<T> when(Predicate cond, T then) {
        return when(cond, Literals.any(then));
    }

    public CaseBuilder<T> when(Predicate cond, Expression<T> then) {
        whens.add(new Tuple2<>(cond, then));
        return this;
    }

    public Expression<T> otherwise(T otherwise) {
        return otherwise(Literals.any(otherwise));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Expression<T> otherwise(Expression<T> otherwise) {
        List<Tuple2<Predicate, Expression<T>>> whens = new ArrayList<>(this.whens);
        if (String.class.isAssignableFrom(type)) {
            return (Expression<T>) new StrExpr(
                    (List<Tuple2<Predicate, Expression<String>>>)(List<?>)whens,
                    (Expression<String>)otherwise
            );
        }
        if (type.isPrimitive() || Number.class.isAssignableFrom(type)) {
            return (Expression<T>) new NumExpr(
                    type,
                    whens,
                    otherwise
            );
        }
        if (Comparable.class.isAssignableFrom(type)) {
            return (Expression<T>) new CmpExpr(
                    type,
                    whens,
                    otherwise
            );
        }
        return new AnyExpr<>(type, whens, otherwise);
    }
    
    public static class Str extends CaseBuilder<String> {

        Str() {
            super(String.class);
        }

        @Override
        public Str when(Predicate cond, String then) {
            return (Str) super.when(cond, then);
        }

        @Override
        public Str when(Predicate cond, Expression<String> then) {
            return (Str) super.when(cond, then);
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
    
    public static class Num<N extends Number & Comparable<N>> extends CaseBuilder<N> {

        Num(Class<N> type) {
            super(type);
        }

        @Override
        public Num<N> when(Predicate cond, N then) {
            return (Num<N>)super.when(cond, then);
        }

        @Override
        public Num<N> when(Predicate cond, Expression<N> then) {
            return (Num<N>)super.when(cond, then);
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
    
    public static class Cmp<T extends Comparable<?>> extends CaseBuilder<T> {

        Cmp(Class<T> type) {
            super(type);
        }

        @Override
        public Cmp<T> when(Predicate cond, T then) {
            return (Cmp<T>) super.when(cond, then);
        }

        @Override
        public Cmp<T> when(Predicate cond, Expression<T> then) {
            return (Cmp<T>) super.when(cond, then);
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

        private Class<T> type;

        private List<Tuple2<Predicate, Expression<T>>> whens;

        private Expression<T> otherwise;

        AnyExpr(
                Class<T> type,
                List<Tuple2<Predicate, Expression<T>>> whens,
                Expression<T> otherwise
        ) {
            this.type = type;
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
            for (Tuple2<Predicate, Expression<T>> when : whens) {
                ((Ast) when.get_1()).accept(visitor);
                ((Ast) when.get_2()).accept(visitor);
            }
            ((Ast) otherwise).accept(visitor);
        }

        @Override
        public void renderTo(@NotNull SqlBuilder builder) {
            usingLowestPrecedence(() -> {
                builder.sql("case");
                for (Tuple2<Predicate, Expression<T>> when : whens) {
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
                List<Tuple2<Predicate, Expression<String>>> whens, 
                Expression<String> otherwise
        ) {
            super(String.class, whens, otherwise);
        }
    }
    
    private static class NumExpr<N extends Number & Comparable<N>> extends AnyExpr<N> implements NumericExpressionImplementor<N> {

        NumExpr(
                Class<N> type,
                List<Tuple2<Predicate, Expression<N>>> whens, 
                Expression<N> otherwise
        ) {
            super(type, whens, otherwise);
        }
    }
    
    private static class CmpExpr<T extends Comparable<?>> extends AnyExpr<T> implements ComparableExpressionImplementor<T> {
        
        CmpExpr(
                Class<T> type, 
                List<Tuple2<Predicate, Expression<T>>> whens,
                Expression<T> otherwise
        ) {
            super(type, whens, otherwise);
        }
    }
}
