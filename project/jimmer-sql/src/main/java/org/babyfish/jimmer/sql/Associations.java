package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.mutation.AssociationSaveCommand;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;

import java.sql.Connection;
import java.util.Collection;

public interface Associations {

    @NewChain
    Associations forConnection(Connection con);

    @NewChain
    Associations reverse();

    @NewChain
    default Associations checkExistence() {
        return checkExistence(true);
    }

    @NewChain
    Associations checkExistence(boolean checkExistence);

    default int save(Object sourceId, Object targetId) {
        return saveCommand(sourceId, targetId).execute();
    }

    default int batchSave(Collection<?> sourceIds, Collection<?> targetIds) {
        return batchSaveCommand(sourceIds, targetIds).execute();
    }

    default int batchSave(Collection<Tuple2<?, ?>> idTuples) {
        return batchSaveCommand(idTuples).execute();
    }
    
    AssociationSaveCommand saveCommand(Object sourceId, Object targetId);

    AssociationSaveCommand batchSaveCommand(Collection<?> sourceIds, Collection<?> targetIds);

    AssociationSaveCommand batchSaveCommand(Collection<Tuple2<?, ?>> idTuples);

    default int delete(Object sourceId, Object targetId) {
        return deleteCommand(sourceId, targetId).execute();
    }

    default int batchDelete(Collection<?> sourceIds, Collection<?> targetIds) {
        return batchDeleteCommand(sourceIds, targetIds).execute();
    }

    default int batchDelete(Collection<Tuple2<?, ?>> idTuples) {
        return batchDeleteCommand(idTuples).execute();
    }

    Executable<Integer> deleteCommand(Object sourceId, Object targetId);

    Executable<Integer> batchDeleteCommand(Collection<?> sourceIds, Collection<?> targetIds);

    Executable<Integer> batchDeleteCommand(Collection<Tuple2<?, ?>> idTuples);
}
