package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.*;

import java.sql.Connection;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private BatchSaveResult<E> executeImpl(Connection con) {
        SaverCache cache = new SaverCache(data);
        Map<AffectedTable, Integer> affectedRowCountMap = new LinkedHashMap<>();
        List<SimpleSaveResult<E>> simpleSaveResults = entities
                .stream()
                .map(it -> new Saver(data, con, cache, affectedRowCountMap).save(it))
                .collect(Collectors.toList());
        return new BatchSaveResult<>(affectedRowCountMap, simpleSaveResults);
    }

    @Override
    BatchEntitySaveCommand<E> create(Data data) {
        return new BatchEntitySaveCommandImpl<>(this, data);
    }
}
