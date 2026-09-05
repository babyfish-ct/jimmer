package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.mutation.AbstractEntitySaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.SaveAssignmentExpression;
import org.babyfish.jimmer.sql.ast.mutation.UnloadedVersionBehavior;
import org.babyfish.jimmer.sql.ast.mutation.UpdateCondition;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface SaveCommandImplementor extends AbstractEntitySaveCommand {

    AbstractEntitySaveCommand setForceMatchedUpdate();

    AbstractEntitySaveCommand setExactConflictTargetRequired(boolean byKey);

    AbstractEntitySaveCommand setEntityOptimisticLock(
            ImmutableType type,
            UnloadedVersionBehavior behavior,
            UpdateCondition<Object, Table<Object>> condition
    );

    AbstractEntitySaveCommand setEntityAssignment(
            ImmutableProp prop,
            SaveAssignmentExpression<?, ?, ?> expression
    );

    AbstractEntitySaveCommand setEntityUpdateWhere(
            ImmutableType type,
            UpdateCondition<?, ?> condition
    );
}
