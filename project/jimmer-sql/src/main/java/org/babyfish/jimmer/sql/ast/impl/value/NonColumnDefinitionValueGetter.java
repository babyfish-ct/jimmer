package org.babyfish.jimmer.sql.ast.impl.value;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.impl.Ast;
import org.babyfish.jimmer.sql.ast.impl.ExpressionImplementor;
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl;
import org.babyfish.jimmer.sql.ast.impl.render.AbstractSqlBuilder;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.Nullable;

public class NonColumnDefinitionValueGetter extends AbstractValueGetter {

    private final ExpressionImplementor<?> expression;

    NonColumnDefinitionValueGetter(
            ExpressionImplementor<?> expression,
            ScalarProvider<Object, Object> scalarProvider,
            String sqlTypeName
    ) {
        super(scalarProvider, sqlTypeName);
        this.expression = expression;
    }

    @Override
    protected Object getRaw(Object value) {
        return value;
    }

    @Override
    public ImmutableProp getValueProp() {
        return null;
    }

    @Override
    public @Nullable String getColumnName() {
        return null;
    }

    @Override
    public boolean isNullable() {
        return true;
    }

    @Override
    public void renderTo(AbstractSqlBuilder<?> builder) {
        Ast ast = (Ast) expression;
        if (!(builder instanceof SqlBuilder)) {
            throw new IllegalStateException(
                    "The \"" +
                            NonColumnDefinitionValueGetter.class.getName() +
                            "\" only supports \"" +
                            SqlBuilder.class.getName() +
                            "\""
            );
        }
        ast.renderTo((SqlBuilder) builder);
    }

    @Override
    public int hashCode() {
        return expression.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NonColumnDefinitionValueGetter)) {
            return false;
        }
        NonColumnDefinitionValueGetter other = (NonColumnDefinitionValueGetter) obj;
        return expression.equals(other.expression);
    }

    @Override
    public String toString() {
        return expression.toString();
    }
}
