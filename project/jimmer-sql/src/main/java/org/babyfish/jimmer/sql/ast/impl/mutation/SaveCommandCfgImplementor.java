package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.table.TableImplementor;
import org.babyfish.jimmer.sql.ast.mutation.AbstractEntitySaveCommand;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.function.BiFunction;

public interface SaveCommandCfgImplementor extends AbstractEntitySaveCommand.Cfg {

    void setEntityOptimisticLock(
            ImmutableType type,
            BiFunction<Table<?>, Object, Predicate> block
    );
}
