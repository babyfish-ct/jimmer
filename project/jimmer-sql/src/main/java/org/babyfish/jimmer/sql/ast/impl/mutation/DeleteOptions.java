package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.mutation.DeleteMode;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

public interface DeleteOptions {

    JSqlClientImplementor getSqlClient();

    DeleteMode getMode();

    DissociateAction getDissociateAction(ImmutableProp backReferenceProp);

    default DeleteOptions toMode(DeleteMode mode) {
        if (getMode() == mode) {
            return this;
        }
        return new DeleteOptionsWrapper(this, mode);
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
    public DeleteMode getMode() {
        return mode;
    }

    @Override
    public DissociateAction getDissociateAction(ImmutableProp backReferenceProp) {
        return raw.getDissociateAction(backReferenceProp);
    }

    private static DeleteOptions unwrap(DeleteOptions options) {
        if (options instanceof DeleteOptionsWrapper) {
            return unwrap(((DeleteOptionsWrapper)options).raw);
        }
        return options;
    }
}
