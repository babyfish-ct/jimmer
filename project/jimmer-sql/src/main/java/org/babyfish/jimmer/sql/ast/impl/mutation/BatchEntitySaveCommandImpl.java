package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.mutation.*;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.sql.Connection;
import java.util.*;
import java.util.function.Consumer;

public class BatchEntitySaveCommandImpl<E>
        extends AbstractEntitySaveCommandImpl
        implements BatchEntitySaveCommand<E> {

    private final Collection<E> entities;

    private final ImmutableType type;

    public BatchEntitySaveCommandImpl(JSqlClientImplementor sqlClient, Connection con, Collection<E> entities) {
        super(sqlClient, con, null);
        ImmutableType type = null;
        for (E entity : entities) {
            if (!(entity instanceof ImmutableSpi)) {
                throw new IllegalArgumentException(
                        "All the elements of entities must be an immutable object"
                );
            }
            ImmutableType entityType = ((ImmutableSpi) entity).__type();
            if (entityType != null && entityType != entityType) {
                throw new IllegalArgumentException(
                        "All the elements of entities must belong to same immutable type"
                );
            }
            type = entityType;
        }
        this.entities = entities;
        this.type = type;
    }

    private BatchEntitySaveCommandImpl(BatchEntitySaveCommandImpl<E> base, Data data) {
        super(base.sqlClient, base.con, data);
        this.entities = base.entities;
        this.type = base.type;
    }

    @SuppressWarnings("unchecked")
    @Override
    public BatchEntitySaveCommand<E> configure(Consumer<Cfg> block) {
        return (BatchEntitySaveCommand<E>) super.configure(block);
    }

    @Override
    public BatchSaveResult<E> execute() {
        if (con != null) {
            return executeImpl(con);
        }
        return sqlClient
                .getConnectionManager()
                .execute(this::executeImpl);
    }

    @Override
    public BatchSaveResult<E> execute(Connection con) {
        if (con != null) {
            return executeImpl(con);
        }
        if (this.con != null) {
            return executeImpl(this.con);
        }
        return sqlClient
                .getConnectionManager()
                .execute(this::executeImpl);
    }

    @SuppressWarnings("unchecked")
    private BatchSaveResult<E> executeImpl(Connection con) {
        if (entities.isEmpty()) {
            return new BatchSaveResult<>(Collections.emptyList());
        }
        SaverCache cache = new SaverCache(data);
        Map<AffectedTable, Integer> affectedRowCountMap = new LinkedHashMap<>();
        int size = entities.size();
        List<SimpleSaveResult<E>> oldSimpleResults = new ArrayList<>(size);
        Saver saver = new Saver(data, con, type, cache, false, affectedRowCountMap);
        List<Object> modifiedEntities = Internal.produceList(
                ((ImmutableSpi) entities.iterator().next()).__type(),
                entities,
                list -> {
                    for (Object o : list) {
                        oldSimpleResults.add(saver.save((E)o));
                    }
                }
        );
        saver.submitTrigger();
        List<SimpleSaveResult<E>> newSimpleResults = new ArrayList<>(size);
        int index = 0;
        for (E entity : entities) {
            SimpleSaveResult<E> oldResult = oldSimpleResults.get(index);
            newSimpleResults.add(
                    new SimpleSaveResult<>(
                            oldResult.getAffectedRowCountMap(),
                            entity,
                            (E)modifiedEntities.get(index)
                    )
            );
            index++;
        }

        return new BatchSaveResult<>(
                affectedRowCountMap,
                newSimpleResults
        );
    }

    @Override
    BatchEntitySaveCommand<E> create(Data data) {
        return new BatchEntitySaveCommandImpl<>(this, data);
    }
}
