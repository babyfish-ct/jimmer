package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.mutation.AssociationSaveCommand;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;

import java.util.Collection;

public interface Associations {

    Associations reverse();

    AssociationSaveCommand saveCommand(
            Object sourceId,
            Object targetId
    );

    AssociationSaveCommand saveCommand(
            Collection<Object> sourceIds,
            Collection<Object> targetIds
    );

    AssociationSaveCommand saveCommand(
            Collection<Tuple2<Object, Object>> idPairs
    );

    default int delete(Object sourceId, Object targetId) {
        return deleteCommand(sourceId, targetId).execute();
    }

    default int delete(Collection<Object> sourceIds, Collection<Object> targetIds) {
        return deleteCommand(sourceIds, targetIds).execute();
    }

    default int delete(Collection<Tuple2<Object, Object>> idPairs) {
        return deleteCommand(idPairs).execute();
    }

    Executable<Integer> deleteCommand(Object sourceId, Object targetId);

    Executable<Integer> deleteCommand(Collection<Object> sourceIds, Collection<Object> targetIds);

    Executable<Integer> deleteCommand(Collection<Tuple2<Object, Object>> idPairs);
}
