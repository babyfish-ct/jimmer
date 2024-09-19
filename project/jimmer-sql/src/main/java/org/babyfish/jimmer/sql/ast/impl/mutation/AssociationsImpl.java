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

    private final boolean deleteUnnecessary;

    public AssociationsImpl(
            JSqlClientImplementor sqlClient,
            Connection con, 
            AssociationType associationType
    ) {
        this(sqlClient, con, associationType, false, false, false);
    }

    private AssociationsImpl(
            JSqlClientImplementor sqlClient,
            Connection con,
            AssociationType associationType,
            boolean reversed,
            boolean checkExistence,
            boolean deleteUnnecessary
    ) {
        this.sqlClient = sqlClient;
        this.con = con;
        this.associationType = associationType;
        this.reversed = reversed;
        this.checkExistence = checkExistence;
        this.deleteUnnecessary = deleteUnnecessary;
    }

    @Override
    public Associations forConnection(Connection con) {
        if (this.con == con) {
            return this;
        }
        return new AssociationsImpl(sqlClient, con, associationType, reversed, checkExistence, deleteUnnecessary);
    }

    @Override
    public Associations reverse() {
        return new AssociationsImpl(sqlClient, con, associationType, !reversed, checkExistence, deleteUnnecessary);
    }

    @Override
    public Associations checkExistence(boolean checkExistence) {
        if (this.checkExistence == checkExistence) {
            return this;
        }
        return new AssociationsImpl(sqlClient, con, associationType, reversed, checkExistence, deleteUnnecessary);
    }

    @Override
    public Associations deleteUnnecessary(boolean deleteUnnecessary) {
        if (this.deleteUnnecessary == deleteUnnecessary) {
            return this;
        }
        return new AssociationsImpl(sqlClient, con, associationType, reversed, checkExistence, deleteUnnecessary);
    }

    @Override
    public AssociationSaveCommand saveCommand(Object sourceId, Object targetId) {
        if (sourceId instanceof Collection<?> || targetId instanceof Collection<?>) {
            throw new IllegalArgumentException(
                    "sourceId or targetId cannot be collection, do you want to call `saveAll/saveAllSaveCommand`?"
            );
        }
        return new AssociationSaveCommandImpl(
                saveExecutable(Collections.singleton(new Tuple2<>(sourceId, targetId)))
        );
    }

    @Override
    public AssociationSaveCommand saveAllCommand(Collection<?> sourceIds, Collection<?> targetIds) {
        return new AssociationSaveCommandImpl(
                saveExecutable(cartesianProduct(sourceIds, targetIds))
        );
    }

    @Override
    public AssociationSaveCommand saveAllCommand(Collection<Tuple2<?, ?>> idTuples) {
        return new AssociationSaveCommandImpl(
                saveExecutable(idTuples)
        );
    }

    @Override
    public Executable<Integer> deleteCommand(Object sourceId, Object targetId) {
        if (sourceId instanceof Collection<?> || targetId instanceof Collection<?>) {
            throw new IllegalArgumentException(
                    "sourceId or targetId cannot be collection, do you want to call `deleteAll/deleteAllCommand`?"
            );
        }
        return deleteExecutable(Collections.singleton(new Tuple2<>(sourceId, targetId)));
    }

    @Override
    public Executable<Integer> deleteAllCommand(Collection<?> sourceIds, Collection<?> targetIds) {
        return deleteExecutable(cartesianProduct(sourceIds, targetIds));
    }
    
    @Override
    public Executable<Integer> deleteAllCommand(Collection<Tuple2<?, ?>> idTuples) {
        return deleteExecutable(idTuples);
    }

    private AssociationExecutable saveExecutable(Collection<Tuple2<?, ?>> idTuples) {
        validate(idTuples);
        return new AssociationExecutable(
                sqlClient,
                con,
                associationType,
                reversed,
                false,
                checkExistence,
                deleteUnnecessary,
                idTuples
        );
    }

    private Executable<Integer> deleteExecutable(Collection<Tuple2<?, ?>> idTuples) {
        validate(idTuples);
        return new AssociationExecutable(
                sqlClient,
                con,
                associationType,
                reversed,
                true,
                checkExistence,
                deleteUnnecessary,
                idTuples
        );
    }

    private Collection<Tuple2<?, ?>> cartesianProduct(
            Collection<?> sourceIds,
            Collection<?> targetIds
    ) {
        Set<Tuple2<?, ?>> idTuples = new LinkedHashSet<>(
                (sourceIds.size() * targetIds.size() * 4 + 2) / 3
        );
        for (Object sourceId : sourceIds) {
            for (Object targetId : targetIds) {
                idTuples.add(new Tuple2<>(sourceId, targetId));
            }
        }
        return idTuples;
    }

    private Collection<Tuple2<?, ?>> validate(Collection<Tuple2<?, ?>> idTuples) {
        Class<?> sourceIdType = associationType.getSourceType().getIdProp().getElementClass();
        Class<?> targetIdType = associationType.getTargetType().getIdProp().getElementClass();
        if (reversed) {
            Class<?> tmp = sourceIdType;
            sourceIdType = targetIdType;
            targetIdType = tmp;
        }
        for (Tuple2<?, ?> idTuple : idTuples) {
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
