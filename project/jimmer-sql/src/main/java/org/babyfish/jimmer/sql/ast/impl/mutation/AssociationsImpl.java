package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.Associations;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.Executable;
import org.babyfish.jimmer.sql.ast.mutation.AssociationSaveCommand;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;

import java.util.*;

public class AssociationsImpl implements Associations {

    private SqlClient sqlClient;

    private AssociationType associationType;

    private boolean reversed;

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
        return new AssociationSaveCommandImpl(
                saveExecutable(validateAndZip(sourceId, targetId))
        );
    }

    @Override
    public AssociationSaveCommand saveCommand(Collection<Object> sourceIds, Collection<Object> targetIds) {
        return new AssociationSaveCommandImpl(
                saveExecutable(validateAndZip(sourceIds, targetIds))
        );
    }

    @Override
    public AssociationSaveCommand saveCommand(Collection<Tuple2<Object, Object>> idPairs) {
        return new AssociationSaveCommandImpl(
                saveExecutable(validate(idPairs))
        );
    }

    @Override
    public Executable<Integer> deleteCommand(Object sourceId, Object targetId) {
        return deleteExecutable(validateAndZip(sourceId, targetId));
    }

    @Override
    public Executable<Integer> deleteCommand(Collection<Object> sourceIds, Collection<Object> targetIds) {
        return deleteExecutable(validateAndZip(sourceIds, targetIds));
    }

    @Override
    public Executable<Integer> deleteCommand(Collection<Tuple2<Object, Object>> idPairs) {
        return deleteExecutable(validate(idPairs));
    }

    private AssociationExecutable saveExecutable(Collection<Tuple2<Object, Object>> idPairs) {
        return new AssociationExecutable(
                sqlClient,
                associationType,
                reversed,
                AssociationExecutable.Mode.INSERT,
                idPairs
        );
    }

    private Executable<Integer> deleteExecutable(Collection<Tuple2<Object, Object>> idPairs) {
        return new AssociationExecutable(
                sqlClient,
                associationType,
                reversed,
                AssociationExecutable.Mode.DELETE,
                idPairs
        );
    }

    private static Collection<Tuple2<Object, Object>> validateAndZip(Object sourceId, Object targetId) {
        return Collections.singleton(
                new Tuple2<>(
                        Objects.requireNonNull(sourceId, "sourceId cannot be null"),
                        Objects.requireNonNull(targetId, "targetId cannot be null")
                )
        );
    }

    private static Collection<Tuple2<Object, Object>> validateAndZip(
            Collection<Object> sourceIds,
            Collection<Object> targetIds
    ) {
        if (sourceIds.size() != targetIds.size()) {
            throw new IllegalArgumentException("sourceIds.size must equal to targetIds.size");
        }
        Iterator<Object> sourceItr = sourceIds.iterator();
        Iterator<Object> targetItr = targetIds.iterator();

        // Set is better choice for deeper code
        Set<Tuple2<Object, Object>> zipped = new LinkedHashSet<>((sourceIds.size() * 4 + 2) / 3);

        while (sourceItr.hasNext() && targetItr.hasNext()) {
            Object sourceId = sourceItr.next();
            Object targetId = targetItr.next();
            if (sourceId == null) {
                throw new IllegalArgumentException("sourceIds cannot contains null");
            }
            if (targetId == null) {
                throw new IllegalArgumentException("targetIds cannot contains null");
            }
            zipped.add(new Tuple2<>(sourceId, targetId));
        }
        return zipped;
    }

    private static Collection<Tuple2<Object, Object>> validate(Collection<Tuple2<Object, Object>> idPairs) {
        for (Tuple2<Object, Object> idPair : idPairs) {
            if (idPair._1() == null) {
                throw new IllegalArgumentException("Id pair with null source id is not acceptable");
            }
            if (idPair._2() == null) {
                throw new IllegalArgumentException("Id pair with null target id is not acceptable");
            }
        }
        return idPairs;
    }
}
