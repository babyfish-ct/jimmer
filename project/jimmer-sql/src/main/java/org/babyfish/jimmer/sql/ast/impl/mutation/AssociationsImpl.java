package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.Associations;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.mutation.AssociationSaveCommand;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.runtime.Converters;

import java.util.*;

public class AssociationsImpl implements Associations {

    private final SqlClient sqlClient;

    private final AssociationType associationType;

    private final boolean reversed;

    public AssociationsImpl(SqlClient sqlClient, AssociationType associationType) {
        this(sqlClient, associationType, false);
    }

    private AssociationsImpl(SqlClient sqlClient, AssociationType associationType, boolean reversed) {
        this.sqlClient = sqlClient;
        this.associationType = associationType;
        this.reversed = reversed;
    }

    @Override
    public Associations reverse() {
        return new AssociationsImpl(sqlClient, associationType, !reversed);
    }

    @Override
    public AssociationSaveCommand saveCommand(Object sourceId, Object targetId) {
        if (sourceId instanceof Collection<?> || targetId instanceof Collection<?>) {
            throw new IllegalArgumentException(
                    "sourceId or targetId cannot be collection, do you want to call 'batchSaveCommand'?"
            );
        }
        return new AssociationSaveCommandImpl(
                saveExecutable(Collections.singleton(new Tuple2<>(sourceId, targetId)))
        );
    }

    @Override
    public AssociationSaveCommand batchSaveCommand(Collection<Object> sourceIds, Collection<Object> targetIds) {
        return new AssociationSaveCommandImpl(
                saveExecutable(cartesianProduct(sourceIds, targetIds))
        );
    }

    @Override
    public AssociationSaveCommand batchSaveCommand(Collection<Tuple2<Object, Object>> idTuples) {
        return new AssociationSaveCommandImpl(
                saveExecutable(idTuples)
        );
    }

    @Override
    public Executable<Integer> deleteCommand(Object sourceId, Object targetId) {
        if (sourceId instanceof Collection<?> || targetId instanceof Collection<?>) {
            throw new IllegalArgumentException(
                    "sourceId or targetId cannot be collection, do you want to call 'batchDeleteCommand'?"
            );
        }
        return deleteExecutable(Collections.singleton(new Tuple2<>(sourceId, targetId)));
    }

    @Override
    public Executable<Integer> batchDeleteCommand(Collection<Object> sourceIds, Collection<Object> targetIds) {
        return deleteExecutable(cartesianProduct(sourceIds, targetIds));
    }

    @Override
    public Executable<Integer> batchDeleteCommand(Collection<Tuple2<Object, Object>> idTuples) {
        return deleteExecutable(idTuples);
    }

    private AssociationExecutable saveExecutable(Collection<Tuple2<Object, Object>> idTuples) {
        validate(idTuples);
        return new AssociationExecutable(
                sqlClient,
                associationType,
                reversed,
                AssociationExecutable.Mode.INSERT,
                idTuples
        );
    }

    private Executable<Integer> deleteExecutable(Collection<Tuple2<Object, Object>> idTuples) {
        validate(idTuples);
        return new AssociationExecutable(
                sqlClient,
                associationType,
                reversed,
                AssociationExecutable.Mode.DELETE,
                idTuples
        );
    }

    private Collection<Tuple2<Object, Object>> cartesianProduct(
            Collection<Object> sourceIds,
            Collection<Object> targetIds
    ) {
        Set<Tuple2<Object, Object>> idTuples = new LinkedHashSet<>(
                (sourceIds.size() * targetIds.size() * 4 + 2) / 3
        );
        for (Object sourceId : sourceIds) {
            for (Object targetId : targetIds) {
                idTuples.add(new Tuple2<>(sourceId, targetId));
            }
        }
        return idTuples;
    }

    private Collection<Tuple2<Object, Object>> validate(Collection<Tuple2<Object, Object>> idTuples) {
        Class<?> sourceIdType = associationType.getSourceType().getIdProp().getElementClass();
        Class<?> targetIdType = associationType.getTargetType().getIdProp().getElementClass();
        if (reversed) {
            Class<?> tmp = sourceIdType;
            sourceIdType = targetIdType;
            targetIdType = tmp;
        }
        for (Tuple2<Object, Object> idTuple : idTuples) {
            if (Converters.tryConvert(idTuple._1(), sourceIdType) == null) {
                throw new IllegalArgumentException(
                        "sourceId \"" +
                                idTuple._1() +
                                "\" does not match the type \"" +
                                sourceIdType.getName() +
                                "\""
                );
            }
            if (Converters.tryConvert(idTuple._2(), targetIdType) == null) {
                throw new IllegalArgumentException(
                        "targetId \"" +
                                idTuple._2() +
                                "\" does not match the type \"" +
                                targetIdType.getName() +
                                "\""
                );
            }
        }
        return idTuples;
    }
}
