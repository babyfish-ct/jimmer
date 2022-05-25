package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SimpleEntitySaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;

import java.sql.Connection;

class SimpleEntitySaveCommandImpl<E>
        extends AbstractEntitySaveCommandImpl<SimpleEntitySaveCommand<E>>
        implements SimpleEntitySaveCommand<E> {

    private E entity;

    SimpleEntitySaveCommandImpl(
            SqlClient sqlClient,
            E entity
    ) {
        super(sqlClient, null);
        this.entity = entity;
    }

    private SimpleEntitySaveCommandImpl(
            SimpleEntitySaveCommandImpl<E> base,
            Data data
    ) {
        super(base.sqlClient, data);
        this.entity = base.entity;
    }

    @Override
    public SimpleSaveResult<E> execute() {
        return sqlClient
                .getConnectionManager()
                .execute(this::execute);
    }

    @Override
    public SimpleSaveResult<E> execute(Connection con) {
        Saver saver = new Saver(data, con);
        return saver.save(entity);
    }

    @Override
    SimpleEntitySaveCommand<E> create(Data data) {
        return new SimpleEntitySaveCommandImpl<>(this, data);
    }
}
