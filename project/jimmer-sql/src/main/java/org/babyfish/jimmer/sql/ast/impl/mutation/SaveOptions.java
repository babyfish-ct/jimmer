package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode;
import org.babyfish.jimmer.sql.ast.mutation.LockMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.mutation.UserOptimisticLock;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.Set;
import java.util.function.Function;

public interface SaveOptions {

    JSqlClientImplementor getSqlClient();

    SaveMode getMode();

    AssociatedSaveMode getAssociatedMode(ImmutableProp prop);

    Triggers getTriggers();

    Set<ImmutableProp> getKeyProps(ImmutableType type);

    LockMode getLockMode();

    UserOptimisticLock<?, ?> getUserOptimisticLock(ImmutableType type);
}
