package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.mutation.AssociationSaveCommand;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;

import java.sql.Connection;
import java.util.Collection;

public interface Associations {

    Associations forConnection(Connection con);

    Associations reverse();

    default int save(Object sourceId, Object targetId) {
        return saveCommand(sourceId, targetId).execute();
    }

    default int batchSave(Collection<Object> sourceIds, Collection<Object> targetIds) {
        return batchSaveCommand(sourceIds, targetIds).execute();
    }

    default int batchSave(Collection<Tuple2<Object, Object>> idTuples) {
        return batchSaveCommand(idTuples).execute();
    }
    
    AssociationSaveCommand saveCommand(Object sourceId, Object targetId);

    AssociationSaveCommand batchSaveCommand(Collection<Object> sourceIds, Collection<Object> targetIds);

    AssociationSaveCommand batchSaveCommand(Collection<Tuple2<Object, Object>> idTuples);

    default int delete(Object sourceId, Object targetId) {
        return deleteCommand(sourceId, targetId).execute();
    }

    default int batchDelete(Collection<Object> sourceIds, Collection<Object> targetIds) {
        return batchDeleteCommand(sourceIds, targetIds).execute();
    }

    default int batchDelete(Collection<Tuple2<Object, Object>> idTuples) {
        return batchDeleteCommand(idTuples).execute();
    }

    Executable<Integer> deleteCommand(Object sourceId, Object targetId);

    Executable<Integer> batchDeleteCommand(Collection<Object> sourceIds, Collection<Object> targetIds);

    Executable<Integer> batchDeleteCommand(Collection<Tuple2<Object, Object>> idTuples);
}
