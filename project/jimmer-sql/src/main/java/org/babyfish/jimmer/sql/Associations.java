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
    default Associations ignoreConflict() {
        return ignoreConflict(true);
    }

    @NewChain
    Associations ignoreConflict(boolean ignoreConflict);

    @NewChain
    default Associations deleteUnnecessary() {
        return deleteUnnecessary(true);
    }

    @NewChain
    Associations deleteUnnecessary(boolean deleteUnnecessary);

    @NewChain
    default Associations setDumbBatchAcceptable() {
        return setDumbBatchAcceptable(true);
    }

    @NewChain
    Associations setDumbBatchAcceptable(boolean acceptable);

    default int insert(Object sourceId, Object targetId) {
        return saveCommand(sourceId, targetId)
                .ignoreConflict(false)
                .deleteUnnecessary(false)
                .execute();
    }

    default int insertIfAbsent(Object sourceId, Object targetId) {
        return saveCommand(sourceId, targetId)
                .ignoreConflict(true)
                .deleteUnnecessary(false)
                .execute();
    }

    default int replace(Object sourceId, Object targetId) {
        return saveCommand(sourceId, targetId)
                .ignoreConflict(true)
                .deleteUnnecessary(true)
                .execute();
    }

    default int insertAll(Collection<?> sourceIds, Collection<?> targetIds) {
        return saveAllCommand(sourceIds, targetIds)
                .ignoreConflict(false)
                .deleteUnnecessary(false)
                .execute();
    }

    default int insertAllIfAbsent(Collection<?> sourceIds, Collection<?> targetIds) {
        return saveAllCommand(sourceIds, targetIds)
                .ignoreConflict(true)
                .deleteUnnecessary(false)
                .execute();
    }

    default int replaceAll(Collection<?> sourceIds, Collection<?> targetIds) {
        return saveAllCommand(sourceIds, targetIds)
                .ignoreConflict(true)
                .deleteUnnecessary(true)
                .execute();
    }

    default int insertAll(Collection<Tuple2<?, ?>> idTuples) {
        return saveAllCommand(idTuples)
                .ignoreConflict(false)
                .deleteUnnecessary(false)
                .execute();
    }

    default int insertAllIfAbsent(Collection<Tuple2<?, ?>> idTuples) {
        return saveAllCommand(idTuples)
                .ignoreConflict(true)
                .deleteUnnecessary(false)
                .execute();
    }

    default int replaceAll(Collection<Tuple2<?, ?>> idTuples) {
        return saveAllCommand(idTuples)
                .ignoreConflict(true)
                .deleteUnnecessary(true)
                .execute();
    }

    default int save(Object sourceId, Object targetId) {
        return saveCommand(sourceId, targetId).execute();
    }

    default int saveAll(Collection<?> sourceIds, Collection<?> targetIds) {
        return saveAllCommand(sourceIds, targetIds).execute();
    }

    default int saveAll(Collection<Tuple2<?, ?>> idTuples) {
        return saveAllCommand(idTuples).execute();
    }
    
    AssociationSaveCommand saveCommand(Object sourceId, Object targetId);

    AssociationSaveCommand saveAllCommand(Collection<?> sourceIds, Collection<?> targetIds);

    AssociationSaveCommand saveAllCommand(Collection<Tuple2<?, ?>> idTuples);

    default int delete(Object sourceId, Object targetId) {
        return deleteCommand(sourceId, targetId).execute();
    }

    default int deleteAll(Collection<?> sourceIds, Collection<?> targetIds) {
        return deleteAllCommand(sourceIds, targetIds).execute();
    }

    default int deleteAll(Collection<Tuple2<?, ?>> idTuples) {
        return deleteAllCommand(idTuples).execute();
    }

    Executable<Integer> deleteCommand(Object sourceId, Object targetId);

    Executable<Integer> deleteAllCommand(Collection<?> sourceIds, Collection<?> targetIds);

    Executable<Integer> deleteAllCommand(Collection<Tuple2<?, ?>> idTuples);
}
