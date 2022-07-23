package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.tuple.*;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Tuples {

    private Tuples() {}

    public static class Expr2<T1, T2>
            extends AbstractExpression<Tuple2<T1, T2>> {

        private Expression<T1> expr1;

        private Expression<T2> expr2;

        public Expr2(
                Expression<T1> expr1,
                Expression<T2> expr2
        ) {
            this.expr1 = Objects.requireNonNull(expr1, "expr1 cannot be null");
            this.expr2 = Objects.requireNonNull(expr2, "expr2 cannot be null");
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<Tuple2<T1, T2>> getType() {
            return (Class<Tuple2<T1, T2>>)(Class<?>)Tuple2.class;
        }

        @Override
        public int precedence() {
            return 0;
        }

        @Override
        public void accept(@NotNull AstVisitor visitor) {
            ((Ast) expr1).accept(visitor);
            ((Ast) expr2).accept(visitor);
        }

        @Override
        public void renderTo(@NotNull SqlBuilder builder) {
            builder.sql("(");
            renderChild((Ast) expr1, builder);
            builder.sql(", ");
            renderChild((Ast) expr2, builder);
            builder.sql(")");
        }
    }

    public static class Expr3<T1, T2, T3>
            extends AbstractExpression<Tuple3<T1, T2, T3>> {

        private Expression<T1> expr1;

        private Expression<T2> expr2;

        private Expression<T3> expr3;

        public Expr3(
                Expression<T1> expr1,
                Expression<T2> expr2,
                Expression<T3> expr3
        ) {
            this.expr1 = Objects.requireNonNull(expr1, "expr1 cannot be null");
            this.expr2 = Objects.requireNonNull(expr2, "expr2 cannot be null");
            this.expr3 = Objects.requireNonNull(expr3, "expr3 cannot be null");
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<Tuple3<T1, T2, T3>> getType() {
            return (Class<Tuple3<T1, T2, T3>>)(Class<?>)Tuple3.class;
        }

        @Override
        public int precedence() {
            return 0;
        }

        @Override
        public void accept(@NotNull AstVisitor visitor) {
            ((Ast) expr1).accept(visitor);
            ((Ast) expr2).accept(visitor);
            ((Ast) expr3).accept(visitor);
        }

        @Override
        public void renderTo(@NotNull SqlBuilder builder) {
            builder.sql("(");
            renderChild((Ast) expr1, builder);
            builder.sql(", ");
            renderChild((Ast) expr2, builder);
            builder.sql(", ");
            renderChild((Ast) expr3, builder);
            builder.sql(")");
        }
    }

    public static class Expr4<T1, T2, T3, T4>
            extends AbstractExpression<Tuple4<T1, T2, T3, T4>> {

        private Expression<T1> expr1;

        private Expression<T2> expr2;

        private Expression<T3> expr3;

        private Expression<T4> expr4;

        public Expr4(
                Expression<T1> expr1,
                Expression<T2> expr2,
                Expression<T3> expr3,
                Expression<T4> expr4
        ) {
            this.expr1 = Objects.requireNonNull(expr1, "expr1 cannot be null");
            this.expr2 = Objects.requireNonNull(expr2, "expr2 cannot be null");
            this.expr3 = Objects.requireNonNull(expr3, "expr3 cannot be null");
            this.expr4 = Objects.requireNonNull(expr4, "expr4 cannot be null");
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<Tuple4<T1, T2, T3, T4>> getType() {
            return (Class<Tuple4<T1, T2, T3, T4>>)(Class<?>)Tuple4.class;
        }

        @Override
        public int precedence() {
            return 0;
        }

        @Override
        public void accept(@NotNull AstVisitor visitor) {
            ((Ast) expr1).accept(visitor);
            ((Ast) expr2).accept(visitor);
            ((Ast) expr3).accept(visitor);
            ((Ast) expr4).accept(visitor);
        }

        @Override
        public void renderTo(@NotNull SqlBuilder builder) {
            builder.sql("(");
            renderChild((Ast) expr1, builder);
            builder.sql(", ");
            renderChild((Ast) expr2, builder);
            builder.sql(", ");
            renderChild((Ast) expr3, builder);
            builder.sql(", ");
            renderChild((Ast) expr4, builder);
            builder.sql(")");
        }
    }

    public static class Expr5<T1, T2, T3, T4, T5>
            extends AbstractExpression<Tuple5<T1, T2, T3, T4, T5>> {

        private Expression<T1> expr1;

        private Expression<T2> expr2;

        private Expression<T3> expr3;

        private Expression<T4> expr4;

        private Expression<T5> expr5;

        public Expr5(
                Expression<T1> expr1,
                Expression<T2> expr2,
                Expression<T3> expr3,
                Expression<T4> expr4,
                Expression<T5> expr5
        ) {
            this.expr1 = Objects.requireNonNull(expr1, "expr1 cannot be null");
            this.expr2 = Objects.requireNonNull(expr2, "expr2 cannot be null");
            this.expr3 = Objects.requireNonNull(expr3, "expr3 cannot be null");
            this.expr4 = Objects.requireNonNull(expr4, "expr4 cannot be null");
            this.expr5 = Objects.requireNonNull(expr5, "expr5 cannot be null");
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<Tuple5<T1, T2, T3, T4, T5>> getType() {
            return (Class<Tuple5<T1, T2, T3, T4, T5>>)(Class<?>)Tuple5.class;
        }

        @Override
        public int precedence() {
            return 0;
        }

        @Override
        public void accept(@NotNull AstVisitor visitor) {
            ((Ast) expr1).accept(visitor);
            ((Ast) expr2).accept(visitor);
            ((Ast) expr3).accept(visitor);
            ((Ast) expr4).accept(visitor);
            ((Ast) expr5).accept(visitor);
        }

        @Override
        public void renderTo(@NotNull SqlBuilder builder) {
            builder.sql("(");
            renderChild((Ast) expr1, builder);
            builder.sql(", ");
            renderChild((Ast) expr2, builder);
            builder.sql(", ");
            renderChild((Ast) expr3, builder);
            builder.sql(", ");
            renderChild((Ast) expr4, builder);
            builder.sql(", ");
            renderChild((Ast) expr5, builder);
            builder.sql(")");
        }
    }

    public static class Expr6<T1, T2, T3, T4, T5, T6>
            extends AbstractExpression<Tuple6<T1, T2, T3, T4, T5, T6>> {

        private Expression<T1> expr1;

        private Expression<T2> expr2;

        private Expression<T3> expr3;

        private Expression<T4> expr4;

        private Expression<T5> expr5;

        private Expression<T6> expr6;

        public Expr6(
                Expression<T1> expr1,
                Expression<T2> expr2,
                Expression<T3> expr3,
                Expression<T4> expr4,
                Expression<T5> expr5,
                Expression<T6> expr6
        ) {
            this.expr1 = Objects.requireNonNull(expr1, "expr1 cannot be null");
            this.expr2 = Objects.requireNonNull(expr2, "expr2 cannot be null");
            this.expr3 = Objects.requireNonNull(expr3, "expr3 cannot be null");
            this.expr4 = Objects.requireNonNull(expr4, "expr4 cannot be null");
            this.expr5 = Objects.requireNonNull(expr5, "expr5 cannot be null");
            this.expr6 = Objects.requireNonNull(expr6, "expr6 cannot be null");
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<Tuple6<T1, T2, T3, T4, T5, T6>> getType() {
            return (Class<Tuple6<T1, T2, T3, T4, T5, T6>>)(Class<?>)Tuple6.class;
        }

        @Override
        public int precedence() {
            return 0;
        }

        @Override
        public void accept(@NotNull AstVisitor visitor) {
            ((Ast) expr1).accept(visitor);
            ((Ast) expr2).accept(visitor);
            ((Ast) expr3).accept(visitor);
            ((Ast) expr4).accept(visitor);
            ((Ast) expr5).accept(visitor);
            ((Ast) expr6).accept(visitor);
        }

        @Override
        public void renderTo(@NotNull SqlBuilder builder) {
            builder.sql("(");
            renderChild((Ast) expr1, builder);
            builder.sql(", ");
            renderChild((Ast) expr2, builder);
            builder.sql(", ");
            renderChild((Ast) expr3, builder);
            builder.sql(", ");
            renderChild((Ast) expr4, builder);
            builder.sql(", ");
            renderChild((Ast) expr5, builder);
            builder.sql(", ");
            renderChild((Ast) expr6, builder);
            builder.sql(")");
        }
    }

    public static class Expr7<T1, T2, T3, T4, T5, T6, T7>
            extends AbstractExpression<Tuple7<T1, T2, T3, T4, T5, T6, T7>> {

        private Expression<T1> expr1;

        private Expression<T2> expr2;

        private Expression<T3> expr3;

        private Expression<T4> expr4;

        private Expression<T5> expr5;

        private Expression<T6> expr6;

        private Expression<T7> expr7;

        public Expr7(
                Expression<T1> expr1,
                Expression<T2> expr2,
                Expression<T3> expr3,
                Expression<T4> expr4,
                Expression<T5> expr5,
                Expression<T6> expr6,
                Expression<T7> expr7
        ) {
            this.expr1 = Objects.requireNonNull(expr1, "expr1 cannot be null");
            this.expr2 = Objects.requireNonNull(expr2, "expr2 cannot be null");
            this.expr3 = Objects.requireNonNull(expr3, "expr3 cannot be null");
            this.expr4 = Objects.requireNonNull(expr4, "expr4 cannot be null");
            this.expr5 = Objects.requireNonNull(expr5, "expr5 cannot be null");
            this.expr6 = Objects.requireNonNull(expr6, "expr6 cannot be null");
            this.expr7 = Objects.requireNonNull(expr7, "expr7 cannot be null");
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<Tuple7<T1, T2, T3, T4, T5, T6, T7>> getType() {
            return (Class<Tuple7<T1, T2, T3, T4, T5, T6, T7>>)(Class<?>)Tuple7.class;
        }

        @Override
        public int precedence() {
            return 0;
        }

        @Override
        public void accept(@NotNull AstVisitor visitor) {
            ((Ast) expr1).accept(visitor);
            ((Ast) expr2).accept(visitor);
            ((Ast) expr3).accept(visitor);
            ((Ast) expr4).accept(visitor);
            ((Ast) expr5).accept(visitor);
            ((Ast) expr6).accept(visitor);
            ((Ast) expr7).accept(visitor);
        }

        @Override
        public void renderTo(@NotNull SqlBuilder builder) {
            builder.sql("(");
            renderChild((Ast) expr1, builder);
            builder.sql(", ");
            renderChild((Ast) expr2, builder);
            builder.sql(", ");
            renderChild((Ast) expr3, builder);
            builder.sql(", ");
            renderChild((Ast) expr4, builder);
            builder.sql(", ");
            renderChild((Ast) expr5, builder);
            builder.sql(", ");
            renderChild((Ast) expr6, builder);
            builder.sql(", ");
            renderChild((Ast) expr7, builder);
            builder.sql(")");
        }
    }

    public static class Expr8<T1, T2, T3, T4, T5, T6, T7, T8>
            extends AbstractExpression<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> {

        private Expression<T1> expr1;

        private Expression<T2> expr2;

        private Expression<T3> expr3;

        private Expression<T4> expr4;

        private Expression<T5> expr5;

        private Expression<T6> expr6;

        private Expression<T7> expr7;

        private Expression<T8> expr8;

        public Expr8(
                Expression<T1> expr1,
                Expression<T2> expr2,
                Expression<T3> expr3,
                Expression<T4> expr4,
                Expression<T5> expr5,
                Expression<T6> expr6,
                Expression<T7> expr7,
                Expression<T8> expr8
        ) {
            this.expr1 = Objects.requireNonNull(expr1, "expr1 cannot be null");
            this.expr2 = Objects.requireNonNull(expr2, "expr2 cannot be null");
            this.expr3 = Objects.requireNonNull(expr3, "expr3 cannot be null");
            this.expr4 = Objects.requireNonNull(expr4, "expr4 cannot be null");
            this.expr5 = Objects.requireNonNull(expr5, "expr5 cannot be null");
            this.expr6 = Objects.requireNonNull(expr6, "expr6 cannot be null");
            this.expr7 = Objects.requireNonNull(expr7, "expr7 cannot be null");
            this.expr8 = Objects.requireNonNull(expr8, "expr8 cannot be null");
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> getType() {
            return (Class<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>>)(Class<?>)Tuple8.class;
        }

        @Override
        public int precedence() {
            return 0;
        }

        @Override
        public void accept(@NotNull AstVisitor visitor) {
            ((Ast) expr1).accept(visitor);
            ((Ast) expr2).accept(visitor);
            ((Ast) expr3).accept(visitor);
            ((Ast) expr4).accept(visitor);
            ((Ast) expr5).accept(visitor);
            ((Ast) expr6).accept(visitor);
            ((Ast) expr7).accept(visitor);
            ((Ast) expr8).accept(visitor);
        }

        @Override
        public void renderTo(@NotNull SqlBuilder builder) {
            builder.sql("(");
            renderChild((Ast) expr1, builder);
            builder.sql(", ");
            renderChild((Ast) expr2, builder);
            builder.sql(", ");
            renderChild((Ast) expr3, builder);
            builder.sql(", ");
            renderChild((Ast) expr4, builder);
            builder.sql(", ");
            renderChild((Ast) expr5, builder);
            builder.sql(", ");
            renderChild((Ast) expr6, builder);
            builder.sql(", ");
            renderChild((Ast) expr7, builder);
            builder.sql(", ");
            renderChild((Ast) expr8, builder);
            builder.sql(")");
        }
    }

    public static class Expr9<T1, T2, T3, T4, T5, T6, T7, T8, T9>
            extends AbstractExpression<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> {

        private Expression<T1> expr1;

        private Expression<T2> expr2;

        private Expression<T3> expr3;

        private Expression<T4> expr4;

        private Expression<T5> expr5;

        private Expression<T6> expr6;

        private Expression<T7> expr7;

        private Expression<T8> expr8;

        private Expression<T9> expr9;

        public Expr9(
                Expression<T1> expr1,
                Expression<T2> expr2,
                Expression<T3> expr3,
                Expression<T4> expr4,
                Expression<T5> expr5,
                Expression<T6> expr6,
                Expression<T7> expr7,
                Expression<T8> expr8,
                Expression<T9> expr9
        ) {
            this.expr1 = Objects.requireNonNull(expr1, "expr1 cannot be null");
            this.expr2 = Objects.requireNonNull(expr2, "expr2 cannot be null");
            this.expr3 = Objects.requireNonNull(expr3, "expr3 cannot be null");
            this.expr4 = Objects.requireNonNull(expr4, "expr4 cannot be null");
            this.expr5 = Objects.requireNonNull(expr5, "expr5 cannot be null");
            this.expr6 = Objects.requireNonNull(expr6, "expr6 cannot be null");
            this.expr7 = Objects.requireNonNull(expr7, "expr7 cannot be null");
            this.expr8 = Objects.requireNonNull(expr8, "expr8 cannot be null");
            this.expr9 = Objects.requireNonNull(expr9, "expr9 cannot be null");
        }

        @SuppressWarnings("unchecked")
        @Override
        public Class<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> getType() {
            return (Class<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>>)(Class<?>)Tuple9.class;
        }

        @Override
        public int precedence() {
            return 0;
        }

        @Override
        public void accept(@NotNull AstVisitor visitor) {
            ((Ast) expr1).accept(visitor);
            ((Ast) expr2).accept(visitor);
            ((Ast) expr3).accept(visitor);
            ((Ast) expr4).accept(visitor);
            ((Ast) expr5).accept(visitor);
            ((Ast) expr6).accept(visitor);
            ((Ast) expr7).accept(visitor);
            ((Ast) expr8).accept(visitor);
            ((Ast) expr9).accept(visitor);
        }

        @Override
        public void renderTo(@NotNull SqlBuilder builder) {
            builder.sql("(");
            renderChild((Ast) expr1, builder);
            builder.sql(", ");
            renderChild((Ast) expr2, builder);
            builder.sql(", ");
            renderChild((Ast) expr3, builder);
            builder.sql(", ");
            renderChild((Ast) expr4, builder);
            builder.sql(", ");
            renderChild((Ast) expr5, builder);
            builder.sql(", ");
            renderChild((Ast) expr6, builder);
            builder.sql(", ");
            renderChild((Ast) expr7, builder);
            builder.sql(", ");
            renderChild((Ast) expr8, builder);
            builder.sql(", ");
            renderChild((Ast) expr9, builder);
            builder.sql(")");
        }
    }
}
