package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.event.Triggers;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.sql.Connection;

public interface DeleteOptions {

    JSqlClientImplementor getSqlClient();

    Connection getConnection();

    DeleteMode getMode();

    DissociateAction getDissociateAction(ImmutableProp backReferenceProp);

    Triggers getTriggers();

    default DeleteOptions toMode(DeleteMode mode) {
        if (getMode() == mode) {
            return this;
        }
        return new DeleteOptionsWrapper(this, mode);
    }

    static DeleteOptions detach(SaveOptions options) {
        return new DetachOptions(options);
    }
}

class DeleteOptionsWrapper implements DeleteOptions {

    private final DeleteOptions raw;

    private final DeleteMode mode;

    DeleteOptionsWrapper(DeleteOptions raw, DeleteMode mode) {
        this.raw = unwrap(raw);
        this.mode = mode;
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
    public DeleteMode getMode() {
        return mode;
    }

    @Override
    public DissociateAction getDissociateAction(ImmutableProp backReferenceProp) {
        return raw.getDissociateAction(backReferenceProp);
    }

    @Override
    public Triggers getTriggers() {
        return raw.getTriggers();
    }

    private static DeleteOptions unwrap(DeleteOptions options) {
        if (options instanceof DeleteOptionsWrapper) {
            return unwrap(((DeleteOptionsWrapper)options).raw);
        }
        return options;
    }
}

class DetachOptions implements DeleteOptions {

    private final SaveOptions saveOptions;

    DetachOptions(SaveOptions saveOptions) {
        this.saveOptions = saveOptions;
    }

    @Override
    public JSqlClientImplementor getSqlClient() {
        return saveOptions.getSqlClient();
    }

    @Override
    public Connection getConnection() {
        return saveOptions.getConnection();
    }

    @Override
    public DeleteMode getMode() {
        return saveOptions.getDeleteMode();
    }

    @Override
    public DissociateAction getDissociateAction(ImmutableProp backReferenceProp) {
        DissociateAction action = saveOptions.getDissociateAction(backReferenceProp);
        switch (action) {
            case CHECK:
            case SET_NULL:
            case DELETE:
                return action;
            default:
                return DissociateAction.DELETE;
        }
    }

    @Override
    public Triggers getTriggers() {
        return saveOptions.getTriggers();
    }
}