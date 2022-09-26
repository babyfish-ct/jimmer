package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.DissociateAction;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.function.Consumer;
import java.util.function.Function;

public interface DeleteCommand extends Executable<DeleteResult> {

    @NewChain
    DeleteCommand configure(Consumer<Cfg> block);

    interface Cfg {

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

        @OldChain
        Cfg setDissociateAction(
                Class<?> entityType,
                String prop,
                DissociateAction dissociateAction
        );

        @OldChain
        <T extends Table<?>> Cfg setDissociateAction(
                Class<T> tableType,
                Function<T, Table<?>> block,
                DissociateAction dissociateAction
        );
    }
}
