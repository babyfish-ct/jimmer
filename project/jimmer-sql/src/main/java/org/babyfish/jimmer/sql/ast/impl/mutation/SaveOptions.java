package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.mutation.AssociatedSaveMode;
import org.babyfish.jimmer.sql.ast.mutation.LockMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.babyfish.jimmer.sql.ast.mutation.UserOptimisticLock;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.util.Set;

public interface SaveOptions {

    JSqlClientImplementor getSqlClient();

    SaveMode getMode();

    AssociatedSaveMode getAssociatedMode(ImmutableProp prop);

    Triggers getTriggers();

    Set<ImmutableProp> getKeyProps(ImmutableType type);

    LockMode getLockMode();

    UserOptimisticLock<?, ?> getUserOptimisticLock(ImmutableType type);

    boolean isAutoCheckingProp(ImmutableProp prop);

    default SaveOptions toMode(SaveMode mode) {
        if (getMode() == mode) {
            return this;
        }
        return new SaveOptionsWrapper(this, mode);
    }
}

class SaveOptionsWrapper implements SaveOptions {

    private final SaveOptions raw;

    private final SaveMode mode;

    SaveOptionsWrapper(SaveOptions raw, SaveMode mode) {
        this.raw = unwrap(raw);
        this.mode = mode;
    }

    @Override
    public JSqlClientImplementor getSqlClient() {
        return raw.getSqlClient();
    }

    @Override
    public SaveMode getMode() {
        return mode;
    }

    @Override
    public AssociatedSaveMode getAssociatedMode(ImmutableProp prop) {
        return raw.getAssociatedMode(prop);
    }

    @Override
    public Triggers getTriggers() {
        return raw.getTriggers();
    }

    @Override
    public Set<ImmutableProp> getKeyProps(ImmutableType type) {
        return raw.getKeyProps(type);
    }

    @Override
    public LockMode getLockMode() {
        return raw.getLockMode();
    }

    @Override
    public UserOptimisticLock<?, ?> getUserOptimisticLock(ImmutableType type) {
        return raw.getUserOptimisticLock(type);
    }

    @Override
    public boolean isAutoCheckingProp(ImmutableProp prop) {
        return raw.isAutoCheckingProp(prop);
    }

    private static SaveOptions unwrap(SaveOptions options) {
        if (options instanceof SaveOptionsWrapper) {
            return unwrap(((SaveOptionsWrapper)options).raw);
        }
        return options;
    }
}
