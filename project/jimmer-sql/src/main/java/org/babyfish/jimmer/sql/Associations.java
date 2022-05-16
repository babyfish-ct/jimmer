package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;

import java.util.Collection;

public interface Associations {

    Associations reverse();

    default Executable<Integer> saveCommand(Object sourceId, Object targetId) {
        return saveCommand(sourceId, targetId, true);
    }

    default Executable<Integer> saveCommand(Collection<Object> sourceIds, Collection<Object> targetIds) {
        return saveCommand(sourceIds, targetIds, true);
    }

    default Executable<Integer> saveCommand(Collection<Tuple2<Object, Object>> idPairs) {
        return saveCommand(idPairs, true);
    }

    Executable<Integer> saveCommand(Object sourceId, Object targetId, boolean checkExistence);

    Executable<Integer> saveCommand(Collection<Object> sourceIds, Collection<Object> targetIds, boolean checkExistence);

    Executable<Integer> saveCommand(Collection<Tuple2<Object, Object>> idPairs, boolean checkExistence);

    Executable<Integer> deleteCommand(Object sourceId, Object targetId);

    Executable<Integer> deleteCommand(Collection<Object> sourceIds, Collection<Object> targetIds);

    Executable<Integer> deleteCommand(Collection<Tuple2<Object, Object>> idPairs);
}
