package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.BatchEntitySaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.BatchSaveResult;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;

import java.sql.Connection;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class BatchEntitySaveCommandImpl<E>
        extends AbstractEntitySaveCommandImpl<BatchEntitySaveCommand<E>>
        implements BatchEntitySaveCommand<E> {

    private Collection<E> entities;

    BatchEntitySaveCommandImpl(SqlClient sqlClient, Collection<E> entities) {
        super(sqlClient, null);
        this.entities = entities;
    }

    private BatchEntitySaveCommandImpl(BatchEntitySaveCommandImpl<E> base, Data data) {
        super(base.sqlClient, data);
        this.entities = base.entities;
    }

    @Override
    public BatchSaveResult<E> execute() {
        return sqlClient
                .getConnectionManager()
                .execute(this::execute);
    }

    @Override
    public BatchSaveResult<E> execute(Connection con) {
        ImmutableCache cache = new ImmutableCache(data);
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
