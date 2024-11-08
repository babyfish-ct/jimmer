package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.mutation.AbstractEntitySaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.UnloadedVersionBehavior;
import org.babyfish.jimmer.sql.ast.mutation.UserOptimisticLock;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface SaveCommandImplementor extends AbstractEntitySaveCommand {

    AbstractEntitySaveCommand setEntityOptimisticLock(
            ImmutableType type,
            UnloadedVersionBehavior behavior,
            UserOptimisticLock<Object, Table<Object>> block
    );
}
