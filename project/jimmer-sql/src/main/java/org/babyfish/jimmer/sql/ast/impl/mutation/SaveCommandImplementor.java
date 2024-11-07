package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.mutation.AbstractEntitySaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.LoadedVersionBehavior;
import org.babyfish.jimmer.sql.ast.mutation.UserOptimisticLock;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface SaveCommandImplementor extends AbstractEntitySaveCommand {

    AbstractEntitySaveCommand setEntityOptimisticLock(
            ImmutableType type,
            LoadedVersionBehavior behavior,
            UserOptimisticLock<Object, Table<Object>> block
    );
}
