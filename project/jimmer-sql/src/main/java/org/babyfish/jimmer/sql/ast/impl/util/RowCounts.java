package org.babyfish.jimmer.sql.ast.impl.util;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;

public interface RowCounts {

    NumericExpression<Long> INSTANCE = Expression.constant(1).count();
}
