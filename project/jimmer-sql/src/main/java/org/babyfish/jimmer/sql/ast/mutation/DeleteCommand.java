package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.OnDeleteAction;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.function.Consumer;
import java.util.function.Function;

public interface DeleteCommand extends Executable<DeleteResult> {

    DeleteCommand configure(Consumer<Cfg> block);

    interface Cfg {

        Cfg setOnDeleteAction(
                ImmutableProp prop,
                OnDeleteAction onDeleteAction
        );

        Cfg setOnDeleteAction(
                Class<?> entityType,
                String prop,
                OnDeleteAction onDeleteAction
        );

        <T extends Table<?>> Cfg setOnDeleteAction(
                Class<T> tableType,
                Function<T, Table<?>> block,
                OnDeleteAction onDeleteAction
        );
    }
}
