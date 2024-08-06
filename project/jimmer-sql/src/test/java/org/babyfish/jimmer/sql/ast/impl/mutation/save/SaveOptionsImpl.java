package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.impl.mutation.SaveOptions;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.Set;

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
    public DeleteMode getDeleteMode() {
        return DeleteMode.PHYSICAL;
    }

    @Override
    public DissociateAction getDissociateAction(ImmutableProp prop) {
        return DissociateAction.NONE;
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
