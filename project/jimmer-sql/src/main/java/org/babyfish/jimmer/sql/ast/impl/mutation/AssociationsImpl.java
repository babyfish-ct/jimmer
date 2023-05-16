package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.Associations;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.mutation.AssociationSaveCommand;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.runtime.Converters;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.sql.Connection;
import java.util.*;

public class AssociationsImpl implements Associations {

    private final JSqlClientImplementor sqlClient;
    
    private final Connection con;

    private final AssociationType associationType;

    private final boolean reversed;

    private final boolean checkExistence;

    public AssociationsImpl(
            JSqlClientImplementor sqlClient,
            Connection con, 
            AssociationType associationType
    ) {
        this(sqlClient, con, associationType, false, false);
    }

    private AssociationsImpl(
            JSqlClientImplementor sqlClient,
            Connection con,
            AssociationType associationType,
            boolean reversed,
            boolean checkExistence
    ) {
        this.sqlClient = sqlClient;
        this.con = con;
        this.associationType = associationType;
        this.reversed = reversed;
        this.checkExistence = checkExistence;
    }

    @Override
    public Associations forConnection(Connection con) {
        if (this.con == con) {
            return this;
        }
        return new AssociationsImpl(sqlClient, con, associationType, reversed, checkExistence);
    }

    @Override
    public Associations reverse() {
        return new AssociationsImpl(sqlClient, con, associationType, !reversed, checkExistence);
    }

    @Override
    public Associations checkExistence(boolean checkExistence) {
        if (this.checkExistence == checkExistence) {
            return this;
        }
        return new AssociationsImpl(sqlClient, con, associationType, reversed, checkExistence);
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
                con,
                associationType,
                reversed,
                false,
                checkExistence,
                idTuples
        );
    }

    private Executable<Integer> deleteExecutable(Collection<Tuple2<Object, Object>> idTuples) {
        validate(idTuples);
        return new AssociationExecutable(
                sqlClient,
                con,
                associationType,
                reversed,
                true,
                checkExistence,
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
            if (Converters.tryConvert(idTuple.get_1(), sourceIdType) == null) {
                throw new IllegalArgumentException(
                        "sourceId \"" +
                                idTuple.get_1() +
                                "\" does not match the type \"" +
                                sourceIdType.getName() +
                                "\""
                );
            }
            if (Converters.tryConvert(idTuple.get_2(), targetIdType) == null) {
                throw new IllegalArgumentException(
                        "targetId \"" +
                                idTuple.get_2() +
                                "\" does not match the type \"" +
                                targetIdType.getName() +
                                "\""
                );
            }
        }
        return idTuples;
    }
}
