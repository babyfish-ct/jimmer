package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.mutation.AssociationSaveCommand;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface Associations {

    @NotNull
    Associations reverse();

    default int save(
            @NotNull Object sourceId,
            @NotNull Object targetId
    ) {
        return saveCommand(sourceId, targetId).execute();
    }

    default int batchSave(
            @NotNull Collection<Object> sourceIds,
            @NotNull Collection<Object> targetIds
    ) {
        return batchSaveCommand(sourceIds, targetIds).execute();
    }

    default int batchSave(
            @NotNull Collection<Tuple2<Object, Object>> idTuples
    ) {
        return batchSaveCommand(idTuples).execute();
    }

    @NotNull
    AssociationSaveCommand saveCommand(
            @NotNull Object sourceId,
            @NotNull Object targetId
    );

    @NotNull
    AssociationSaveCommand batchSaveCommand(
            @NotNull Collection<Object> sourceIds,
            @NotNull Collection<Object> targetIds
    );

    @NotNull
    AssociationSaveCommand batchSaveCommand(
            @NotNull Collection<Tuple2<Object, Object>> idTuples
    );

    default int delete(
            @NotNull Object sourceId,
            @NotNull Object targetId
    ) {
        return deleteCommand(sourceId, targetId).execute();
    }

    default int batchDelete(
            @NotNull Collection<Object> sourceIds,
            @NotNull Collection<Object> targetIds
    ) {
        return batchDeleteCommand(sourceIds, targetIds).execute();
    }

    default int batchDelete(
            @NotNull Collection<Tuple2<Object, Object>> idTuples
    ) {
        return batchDeleteCommand(idTuples).execute();
    }

    @NotNull
    Executable<Integer> deleteCommand(
            @NotNull Object sourceId,
            @NotNull Object targetId
    );

    @NotNull
    Executable<Integer> batchDeleteCommand(
            @NotNull Collection<Object> sourceIds,
            @NotNull Collection<Object> targetIds
    );

    @NotNull
    Executable<Integer> batchDeleteCommand(
            @NotNull Collection<Tuple2<Object, Object>> idTuples
    );
}
