package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.Executable;

import java.util.function.Consumer;

public interface DeleteCommand extends Executable<DeleteResult> {

    @NewChain
    DeleteCommand setMode(DeleteMode mode);

    @NewChain
    default DeleteCommand setDissociateAction(TypedProp.Reference<?, ?> prop, DissociateAction dissociateAction) {
        return setDissociateAction(prop.unwrap(), dissociateAction);
    }

    @NewChain
    DeleteCommand setDissociateAction(ImmutableProp prop, DissociateAction dissociateAction);

    @NewChain
    default DeleteCommand setDumbBatchAcceptable() {
        return setDumbBatchAcceptable(true);
    }

    @NewChain
    DeleteCommand setDumbBatchAcceptable(boolean acceptable);
}
