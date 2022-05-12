package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.mutation.BatchSaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.BatchSaveResult;

import java.sql.Connection;
import java.util.Collection;

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
        return null;
    }

    @Override
    BatchSaveCommand<E> create(Data data) {
        return new BatchSaveCommandImpl<>(this, data);
    }
}
