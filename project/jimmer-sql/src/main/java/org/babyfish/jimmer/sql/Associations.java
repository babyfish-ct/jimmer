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

    default int saveAll(Collection<?> sourceIds, Collection<?> targetIds) {
        return saveAllCommand(sourceIds, targetIds).execute();
    }

    default int saveAll(Collection<Tuple2<?, ?>> idTuples) {
        return saveAllCommand(idTuples).execute();
    }

    /**
     * Will be deleted since 1.0, please use {@link #saveAll(Collection, Collection)}
     */
    default int batchSave(Collection<?> sourceIds, Collection<?> targetIds) {
        return batchSaveCommand(sourceIds, targetIds).execute();
    }

    /**
     * Will be deleted since 1.0, please use {@link #saveAll(Collection)}
     */
    default int batchSave(Collection<Tuple2<?, ?>> idTuples) {
        return batchSaveCommand(idTuples).execute();
    }
    
    AssociationSaveCommand saveCommand(Object sourceId, Object targetId);

    AssociationSaveCommand saveAllCommand(Collection<?> sourceIds, Collection<?> targetIds);

    AssociationSaveCommand saveAllCommand(Collection<Tuple2<?, ?>> idTuples);

    /**
     * Will be deleted since 1.0, please use {@link #saveAllCommand(Collection, Collection)}
     */
    default AssociationSaveCommand batchSaveCommand(Collection<?> sourceIds, Collection<?> targetIds) {
        return saveAllCommand(sourceIds, targetIds);
    }

    /**
     * Will be deleted since 1.0, please use {@link #saveAllCommand(Collection)}
     */
    default AssociationSaveCommand batchSaveCommand(Collection<Tuple2<?, ?>> idTuples) {
        return saveAllCommand(idTuples);
    }

    default int delete(Object sourceId, Object targetId) {
        return deleteCommand(sourceId, targetId).execute();
    }

    default int deleteAll(Collection<?> sourceIds, Collection<?> targetIds) {
        return deleteAllCommand(sourceIds, targetIds).execute();
    }

    default int deleteAll(Collection<Tuple2<?, ?>> idTuples) {
        return deleteAllCommand(idTuples).execute();
    }

    /**
     * Will be deleted since 1.0, please use {@link #deleteAll(Collection, Collection)}
     */
    default int batchDelete(Collection<?> sourceIds, Collection<?> targetIds) {
        return batchDeleteCommand(sourceIds, targetIds).execute();
    }

    /**
     * Will be deleted since 1.0, please use {@link #deleteAll(Collection)}
     */
    default int batchDelete(Collection<Tuple2<?, ?>> idTuples) {
        return batchDeleteCommand(idTuples).execute();
    }

    Executable<Integer> deleteCommand(Object sourceId, Object targetId);

    Executable<Integer> deleteAllCommand(Collection<?> sourceIds, Collection<?> targetIds);

    Executable<Integer> deleteAllCommand(Collection<Tuple2<?, ?>> idTuples);

    /**
     * Will be deleted since 1.0, please use {@link #deleteAllCommand(Collection, Collection)}
     */
    default Executable<Integer> batchDeleteCommand(Collection<?> sourceIds, Collection<?> targetIds) {
        return deleteAllCommand(sourceIds, targetIds);
    }

    /**
     * Will be deleted since 1.0, please use {@link #deleteAllCommand(Collection)}
     */
    default Executable<Integer> batchDeleteCommand(Collection<Tuple2<?, ?>> idTuples) {
        return deleteAllCommand(idTuples);
    }
}
