package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.mutation.MutableDelete;
import org.babyfish.jimmer.sql.ast.mutation.MutableUpdate;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;

import java.util.function.BiConsumer;

public class Mutations {

    private Mutations() {}

    public static <T extends TableEx<?>> Executable<Integer> createUpdate(
            SqlClient sqlClient,
            Class<T> tableType,
            BiConsumer<MutableUpdate, T> block
    ) {
        MutableUpdateImpl update = new MutableUpdateImpl(
                sqlClient,
                ImmutableType.get(tableType)
        );
        block.accept(update, update.getTable());
        update.freeze();
        return update;
    }

    public static <T extends TableEx<?>> Executable<Integer> createDelete(
            SqlClient sqlClient,
            Class<T> tableType,
            BiConsumer<MutableDelete, T> block
    ) {
        MutableDeleteImpl delete = new MutableDeleteImpl(
                sqlClient,
                ImmutableType.get(tableType)
        );
        block.accept(delete, delete.getTable());
        delete.freeze();
        return delete;
    }
}
