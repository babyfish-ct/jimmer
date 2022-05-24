package org.babyfish.jimmer.sql.ast.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.CascadeAction;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.function.Consumer;
import java.util.function.Function;

public interface DeleteCommand extends Executable<DeleteResult> {

    DeleteCommand configure(Consumer<Cfg> block);

    interface Cfg {

        Cfg setCascadeAction(
                ImmutableProp prop,
                CascadeAction cascadeAction
        );

        Cfg setCascadeAction(
                Class<?> entityType,
                String prop,
                CascadeAction cascadeAction
        );

        <T extends Table<?>> Cfg setCascadeAction(
                Class<T> tableType,
                Function<T, Table<?>> block,
                CascadeAction cascadeAction
        );
    }
}
