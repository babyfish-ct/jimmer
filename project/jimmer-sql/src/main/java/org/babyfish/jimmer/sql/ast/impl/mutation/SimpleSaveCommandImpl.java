package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;

import java.sql.Connection;

class SimpleSaveCommandImpl<E>
        extends AbstractSaveCommandImpl<SimpleSaveCommand<E>>
        implements SimpleSaveCommand<E> {

    private E entity;

    SimpleSaveCommandImpl(
            SqlClient sqlClient,
            E entity
    ) {
        super(sqlClient, null);
        this.entity = entity;
    }

    private SimpleSaveCommandImpl(
            SimpleSaveCommandImpl<E> base,
            Data data
    ) {
        super(base.sqlClient, data);
        this.entity = base.entity;
    }

    @Override
    public SimpleSaveResult<E> execute(Connection con) {
        Saver saver = new Saver(data, con);
        return saver.save(entity);
    }

    @Override
    SimpleSaveCommand<E> create(Data data) {
        return new SimpleSaveCommandImpl<>(this, data);
    }
}
