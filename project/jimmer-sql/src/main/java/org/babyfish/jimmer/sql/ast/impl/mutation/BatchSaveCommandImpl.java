package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.mutation.BatchSaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.BatchSaveResult;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;

import java.sql.Connection;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class BatchSaveCommandImpl<E>
        extends AbstractSaveCommandImpl<BatchSaveCommand<E>>
        implements BatchSaveCommand<E> {

    private Collection<E> entities;

    BatchSaveCommandImpl(SqlClient sqlClient, Collection<E> entities) {
        super(sqlClient, null);
        this.entities = entities;
    }

    private BatchSaveCommandImpl(BatchSaveCommandImpl<E> base, Data data) {
        super(base.sqlClient, data);
        this.entities = base.entities;
    }

    @Override
    public BatchSaveResult<E> execute(Connection con) {
        ImmutableCache cache = new ImmutableCache(data);
        Map<String, Integer> affectedRowCountMap = new LinkedHashMap<>();
        List<SimpleSaveResult<E>> simpleSaveResults = entities
                .stream()
                .map(it -> new Saver(data, con, cache, affectedRowCountMap).save(it))
                .collect(Collectors.toList());
        return new BatchSaveResult<>(affectedRowCountMap, simpleSaveResults);
    }

    @Override
    BatchSaveCommand<E> create(Data data) {
        return new BatchSaveCommandImpl<>(this, data);
    }
}
