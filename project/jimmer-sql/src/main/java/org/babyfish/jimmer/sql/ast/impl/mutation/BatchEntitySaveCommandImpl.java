package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.*;

import java.lang.reflect.ReflectPermission;
import java.sql.Connection;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BatchEntitySaveCommandImpl<E>
        extends AbstractEntitySaveCommandImpl
        implements BatchEntitySaveCommand<E> {

    private Collection<E> entities;

    public BatchEntitySaveCommandImpl(JSqlClient sqlClient, Connection con, Collection<E> entities) {
        super(sqlClient, con, null);
        for (E entity : entities) {
            if (!(entity instanceof ImmutableSpi)) {
                throw new IllegalArgumentException(
                        "All the elements of entities must be an immutable object"
                );
            }
        }
        this.entities = entities;
    }

    private BatchEntitySaveCommandImpl(BatchEntitySaveCommandImpl<E> base, Data data) {
        super(base.sqlClient, base.con, data);
        this.entities = base.entities;
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
        List<SimpleSaveResult<E>>[] oldResultListRef = new List[1];
        List<Object> modifiedEntities = Internal.produceList(
                ((ImmutableSpi) entities.iterator().next()).__type(),
                entities,
                list -> {
                    oldResultListRef[0] =
                            entities
                                    .stream()
                                    .map(
                                            it -> new Saver(data, con, cache, affectedRowCountMap)
                                                    .save(it)
                                    )
                                    .collect(Collectors.toList());
                }
        );
        int size = oldResultListRef[0].size();
        List<SimpleSaveResult<E>> results = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            SimpleSaveResult<E> result = oldResultListRef[0].get(i);
            results.add(result.copy((E)modifiedEntities.get(i)));
        }
        return new BatchSaveResult<>(
                affectedRowCountMap,
                results
        );
    }

    @Override
    BatchEntitySaveCommand<E> create(Data data) {
        return new BatchEntitySaveCommandImpl<>(this, data);
    }
}
