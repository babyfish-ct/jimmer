package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.mutation.UpdateCondition;

final class TypedUpdateCondition {

    final ImmutableType type;

    final UpdateCondition<?, ?> condition;

    TypedUpdateCondition(ImmutableType type, UpdateCondition<?, ?> condition) {
        this.type = type;
        this.condition = condition;
    }
}
