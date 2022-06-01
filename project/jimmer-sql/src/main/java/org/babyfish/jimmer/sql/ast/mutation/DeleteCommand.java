package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.DeleteAction;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.function.Consumer;
import java.util.function.Function;

public interface DeleteCommand extends Executable<DeleteResult> {

    @NewChain
    DeleteCommand configure(Consumer<Cfg> block);

    interface Cfg {

        @OldChain
        Cfg setDeleteAction(
                ImmutableProp prop,
                DeleteAction deleteAction
        );

        @OldChain
        Cfg setDeleteAction(
                Class<?> entityType,
                String prop,
                DeleteAction deleteAction
        );

        @OldChain
        <T extends Table<?>> Cfg setDeleteAction(
                Class<T> tableType,
                Function<T, Table<?>> block,
                DeleteAction deleteAction
        );
    }
}
