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
    DeleteCommand configure(Consumer<Cfg> block);

    @NewChain
    default DeleteCommand setMode(DeleteMode mode) {
        return configure(it -> it.setMode(mode));
    }

    @NewChain
    default DeleteCommand setDissociateAction(TypedProp.Reference<?, ?> prop, DissociateAction dissociateAction) {
        return configure(it -> it.setDissociateAction(prop, dissociateAction));
    }

    @NewChain
    default DeleteCommand setDissociateAction(ImmutableProp prop, DissociateAction dissociateAction) {
        return configure(it -> it.setDissociateAction(prop, dissociateAction));
    }

    interface Cfg {

        @OldChain
        Cfg setMode(DeleteMode mode);

        @OldChain
        default Cfg setDissociateAction(
                TypedProp.Reference<?, ?> prop,
                DissociateAction dissociateAction
        ) {
            return setDissociateAction(prop.unwrap(), dissociateAction);
        }

        @OldChain
        Cfg setDissociateAction(
                ImmutableProp prop,
                DissociateAction dissociateAction
        );
    }
}
