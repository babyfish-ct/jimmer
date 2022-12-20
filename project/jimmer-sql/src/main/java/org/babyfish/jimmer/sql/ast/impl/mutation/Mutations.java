package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.mutation.MutableDelete;
import org.babyfish.jimmer.sql.ast.mutation.MutableUpdate;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.function.BiConsumer;

public class Mutations {

    private Mutations() {}

    public static <T extends Table<?>> Executable<Integer> createUpdate(
            JSqlClient sqlClient,
            Class<T> tableType,
            BiConsumer<MutableUpdate, T> block
    ) {
        MutableUpdateImpl update = new MutableUpdateImpl(
                sqlClient,
                ImmutableType.get(tableType)
        );
        block.accept(update, update.getTable());
        return update;
    }

    public static Executable<Integer> createUpdate(
            JSqlClient sqlClient,
            ImmutableType type,
            BiConsumer<MutableUpdate, Table<?>> block
    ) {
        MutableUpdateImpl update = new MutableUpdateImpl(
                sqlClient,
                type
        );
        block.accept(update, update.getTable());
        return update;
    }

    public static <T extends Table<?>> Executable<Integer> createDelete(
            JSqlClient sqlClient,
            Class<T> tableType,
            BiConsumer<MutableDelete, T> block
    ) {
        MutableDeleteImpl delete = new MutableDeleteImpl(
                sqlClient,
                ImmutableType.get(tableType)
        );
        block.accept(delete, delete.getTable());
        return delete;
    }

    public static <T extends Table<?>> Executable<Integer> createDelete(
            JSqlClient sqlClient,
            ImmutableType type,
            BiConsumer<MutableDelete, T> block
    ) {
        MutableDeleteImpl delete = new MutableDeleteImpl(
                sqlClient,
                type
        );
        block.accept(delete, delete.getTable());
        return delete;
    }
}
