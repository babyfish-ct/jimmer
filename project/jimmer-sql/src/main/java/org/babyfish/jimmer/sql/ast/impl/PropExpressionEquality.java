package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.impl.base.BaseTableOwner;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;

import java.util.Objects;

/**
 * Property identity is independent of API wrappers and nullability views.
 * Base-query exports also retain their query and projection position.
 */
public final class PropExpressionEquality {

    private PropExpressionEquality() {}

    public static boolean equals(PropExpressionImplementor<?> expression, Object other) {
        if (expression == other) {
            return true;
        }
        if (!(other instanceof PropExpressionImplementor<?>)) {
            return false;
        }
        PropExpressionImplementor<?> that = (PropExpressionImplementor<?>) other;
        return expression.getTable().equals(that.getTable()) &&
                expression.getProp().equals(that.getProp()) &&
                Objects.equals(expression.getPath(), that.getPath()) &&
                Objects.equals(BaseTableOwner.of(expression), BaseTableOwner.of(that));
    }

    public static int hashCode(PropExpressionImplementor<?> expression) {
        return Objects.hash(expression.getTable(), expression.getProp(), expression.getPath(), BaseTableOwner.of(expression));
    }
}
