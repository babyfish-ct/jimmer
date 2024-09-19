package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.runtime.*;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.*;

class AssociationExecutable implements Executable<Integer> {

    final JSqlClientImplementor sqlClient;

    final Connection con;

    private final AssociationType associationType;

    private final boolean reversed;

    private final boolean forDelete;

    private final boolean defaultCheckExistence;

    private final boolean defaultDeleteUnnecessary;

    private final Boolean nullOrCheckedExistence;

    private final Boolean nullOrDeleteUnnecessary;

    private final Set<Tuple2<?, ?>> idTuples;

    public AssociationExecutable(
            JSqlClientImplementor sqlClient,
            Connection con,
            AssociationType associationType,
            boolean reversed,
            boolean forDelete,
            boolean defaultCheckExistence,
            boolean defaultDeleteUnnecessary,
            Collection<Tuple2<?, ?>> idTuples
    ) {
        this(
                sqlClient,
                con,
                associationType,
                reversed,
                forDelete,
                defaultCheckExistence,
                defaultDeleteUnnecessary,
                null,
                null,
                idTuples
        );
    }

    private AssociationExecutable(
            JSqlClientImplementor sqlClient,
            Connection con,
            AssociationType associationType,
            boolean reversed,
            boolean forDelete,
            boolean defaultCheckExistence,
            boolean defaultDeleteUnnecessary,
            Boolean nullOrCheckedExistence,
            Boolean nullOrDeleteUnnecessary,
            Collection<Tuple2<?, ?>> idTuples
    ) {
        this.sqlClient = sqlClient;
        this.con = con;
        this.associationType = associationType;
        this.reversed = reversed;
        this.forDelete = forDelete;
        this.defaultCheckExistence = defaultCheckExistence;
        this.defaultDeleteUnnecessary = defaultDeleteUnnecessary;
        this.nullOrCheckedExistence = nullOrCheckedExistence;
        this.nullOrDeleteUnnecessary = nullOrDeleteUnnecessary;
        this.idTuples = idTuples instanceof Set<?> ?
                (Set<Tuple2<?, ?>>) idTuples :
                new LinkedHashSet<>(idTuples);
    }

    @NewChain
    public AssociationExecutable setCheckExistence(@Nullable Boolean checkExistence) {
        if (nullOrCheckedExistence == checkExistence) {
            return this;
        }
        return new AssociationExecutable(
                sqlClient,
                con,
                associationType,
                reversed,
                forDelete,
                defaultCheckExistence,
                defaultDeleteUnnecessary,
                checkExistence,
                nullOrDeleteUnnecessary,
                idTuples
        );
    }

    @NewChain
    public AssociationExecutable setDeleteUnnecessary(@Nullable Boolean deleteUnnecessary) {
        if (nullOrDeleteUnnecessary == deleteUnnecessary) {
            return this;
        }
        return new AssociationExecutable(
                sqlClient,
                con,
                associationType,
                reversed,
                forDelete,
                defaultCheckExistence,
                defaultDeleteUnnecessary,
                nullOrCheckedExistence,
                deleteUnnecessary,
                idTuples
        );
    }

    @Override
    public Integer execute(Connection con) {
        return sqlClient
                .getConnectionManager()
                .execute(con == null ? this.con : con, this::executeImpl);
    }

    @SuppressWarnings("unchecked")
    private Integer executeImpl(Connection con) {

        if (idTuples.isEmpty()) {
            return 0;
        }

        MutationPath path;
        if (reversed) {
            path = MutationPath
                    .root(associationType.getBaseProp().getTargetType())
                    .backFrom(associationType.getBaseProp());
        } else {
            path = MutationPath
                    .root(associationType.getBaseProp().getDeclaringType())
                    .to(associationType.getBaseProp());
        }
        MutationTrigger trigger = null;
        if (sqlClient.getTriggerType() != TriggerType.BINLOG_ONLY) {
            trigger = new MutationTrigger();
        }
        Map<AffectedTable, Integer> affectedRowCountMap = new HashMap<>();
        MiddleTableOperator operator = new MiddleTableOperator(
                sqlClient,
                con,
                path,
                sqlClient.getExceptionTranslator(),
                trigger,
                affectedRowCountMap,
                null,
                false
        );

        boolean checkExistence = nullOrCheckedExistence != null ?
                nullOrCheckedExistence :
                defaultCheckExistence;
        boolean deleteUnnecessary = nullOrDeleteUnnecessary != null ?
                nullOrDeleteUnnecessary :
                defaultDeleteUnnecessary;
        IdPairs idPairs = IdPairs.of((Collection<Tuple2<Object, Object>>) (Collection<?>) idTuples);
        if (forDelete) {
            operator.delete(idPairs);
        } else {
            if (deleteUnnecessary) {
                operator.disconnectExcept(IdPairs.retain(idPairs));
            }
            if (checkExistence) {
                operator.merge(idPairs);
            } else {
                operator.append(idPairs);
            }
        }

        if (trigger != null) {
            trigger.submit(sqlClient, con);
        }

        int affectedRowCount = 0;
        for (Integer rowCount : affectedRowCountMap.values()) {
            affectedRowCount += rowCount;
        }
        return affectedRowCount;
    }
}
