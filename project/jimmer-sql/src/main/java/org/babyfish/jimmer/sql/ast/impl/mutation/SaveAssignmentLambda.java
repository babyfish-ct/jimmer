package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.mutation.SaveAssignmentExpression;

final class SaveAssignmentLambda {

    final ImmutableType type;

    final SaveAssignmentExpression<?, ?, ?> expression;

    SaveAssignmentLambda(
            ImmutableType type,
            SaveAssignmentExpression<?, ?, ?> expression
    ) {
        this.type = type;
        this.expression = expression;
    }
}
