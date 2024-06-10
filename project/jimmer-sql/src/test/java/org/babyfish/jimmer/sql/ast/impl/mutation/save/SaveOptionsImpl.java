package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.impl.mutation.SaveOptions;
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode;
import org.babyfish.jimmer.sql.ast.mutation.LockMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.mutation.UserOptimisticLock;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.Set;
import java.util.function.Function;

public class SaveOptionsImpl implements SaveOptions {

    JSqlClientImplementor sqlClient;

    SaveMode mode = SaveMode.UPSERT;

    AssociatedSaveMode associatedMode = AssociatedSaveMode.REPLACE;

    UserOptimisticLock<?, ?> userOptimisticLock;

    public SaveOptionsImpl(JSqlClientImplementor sqlClient) {
        this.sqlClient = sqlClient;
    }

    @Override
    public JSqlClientImplementor getSqlClient() {
        return sqlClient;
    }

    @Override
    public SaveMode getMode() {
        return mode;
    }

    @Override
    public AssociatedSaveMode getAssociatedMode(ImmutableProp prop) {
        return associatedMode;
    }

    @Override
    public Triggers getTriggers() {
        return sqlClient.getTriggerType() == TriggerType.BINLOG_ONLY ?
                null :
                sqlClient.getTriggers(true);
    }

    @Override
    public Set<ImmutableProp> getKeyProps(ImmutableType type) {
        return type.getKeyProps();
    }

    @Override
    public LockMode getLockMode() {
        return LockMode.AUTO;
    }

    @Override
    public UserOptimisticLock<?, ?> getUserOptimisticLock(ImmutableType type) {
        return userOptimisticLock;
    }

    @Override
    public boolean isAutoCheckingProp(ImmutableProp prop) {
        return false;
    }
}
