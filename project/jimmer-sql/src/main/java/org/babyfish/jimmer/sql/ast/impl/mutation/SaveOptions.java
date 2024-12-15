package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.KeyMatcher;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.runtime.ExceptionTranslator;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;

public interface SaveOptions {

    JSqlClientImplementor getSqlClient();

    Connection getConnection();

    SaveMode getMode();

    AssociatedSaveMode getAssociatedMode(ImmutableProp prop);

    Triggers getTriggers();

    KeyMatcher getKeyMatcher(ImmutableType type);

    DeleteMode getDeleteMode();

    DissociateAction getDissociateAction(ImmutableProp prop);

    boolean isTargetTransferable(ImmutableProp prop);

    boolean isPessimisticLocked(ImmutableType type);

    UnloadedVersionBehavior getUnloadedVersionBehavior(ImmutableType type);

    UserOptimisticLock<?, ?> getUserOptimisticLock(ImmutableType type);

    boolean isAutoCheckingProp(ImmutableProp prop);

    boolean isKeyOnlyAsReference(ImmutableProp prop);

    boolean isBatchForbidden();

    @Nullable
    ExceptionTranslator<Exception> getExceptionTranslator();

    default SaveOptions withMode(SaveMode mode) {
        if (getMode() == mode) {
            return this;
        }
        return new SaveOptionsWithMode(this, mode);
    }

    default SaveOptions withSqlClient(JSqlClientImplementor sqlClient) {
        if (getSqlClient() == sqlClient) {
            return this;
        }
        return new SaveOptionsWithSqlClient(this, sqlClient);
    }
}

abstract class AbstractSaveOptionsWrapper implements SaveOptions {

    private final SaveOptions raw;

    AbstractSaveOptionsWrapper(SaveOptions raw) {
        this.raw = unwrap(raw);
    }

    @Override
    public SaveMode getMode() {
        return raw.getMode();
    }

    @Override
    public JSqlClientImplementor getSqlClient() {
        return raw.getSqlClient();
    }

    @Override
    public Connection getConnection() {
        return raw.getConnection();
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
    public KeyMatcher getKeyMatcher(ImmutableType type) {
        return raw.getKeyMatcher(type);
    }

    @Override
    public boolean isTargetTransferable(ImmutableProp prop) {
        return raw.isTargetTransferable(prop);
    }

    @Override
    public DeleteMode getDeleteMode() {
        return raw.getDeleteMode();
    }

    @Override
    public DissociateAction getDissociateAction(ImmutableProp prop) {
        return raw.getDissociateAction(prop);
    }

    @Override
    public boolean isPessimisticLocked(ImmutableType type) {
        return raw.isPessimisticLocked(type);
    }

    @Override
    public UnloadedVersionBehavior getUnloadedVersionBehavior(ImmutableType type) {
        return raw.getUnloadedVersionBehavior(type);
    }

    @Override
    public UserOptimisticLock<?, ?> getUserOptimisticLock(ImmutableType type) {
        return raw.getUserOptimisticLock(type);
    }

    @Override
    public boolean isAutoCheckingProp(ImmutableProp prop) {
        return raw.isAutoCheckingProp(prop);
    }

    @Override
    public boolean isKeyOnlyAsReference(ImmutableProp prop) {
        return raw.isKeyOnlyAsReference(prop);
    }

    @Override
    public boolean isBatchForbidden() {
        return raw.isBatchForbidden();
    }

    @Override
    @Nullable
    public ExceptionTranslator<Exception> getExceptionTranslator() {
        return raw.getExceptionTranslator();
    }

    private static SaveOptions unwrap(SaveOptions options) {
        if (options instanceof AbstractSaveOptionsWrapper) {
            return unwrap(((AbstractSaveOptionsWrapper)options).raw);
        }
        return options;
    }
}

class SaveOptionsWithMode extends AbstractSaveOptionsWrapper {

    final SaveMode mode;

    SaveOptionsWithMode(SaveOptions raw, SaveMode mode) {
        super(raw);
        this.mode = mode;
    }

    @Override
    public SaveMode getMode() {
        return mode;
    }
}

class SaveOptionsWithSqlClient extends AbstractSaveOptionsWrapper {

    private final JSqlClientImplementor sqlClient;

    SaveOptionsWithSqlClient(SaveOptions raw, JSqlClientImplementor sqlClient) {
        super(raw);
        this.sqlClient = sqlClient;
    }

    @Override
    public JSqlClientImplementor getSqlClient() {
        return sqlClient;
    }
}

