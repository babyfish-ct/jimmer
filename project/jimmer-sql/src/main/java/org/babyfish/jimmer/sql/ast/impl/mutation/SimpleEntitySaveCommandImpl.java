package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.SqlClient;
import org.babyfish.jimmer.sql.ast.mutation.AbstractEntitySaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.SimpleEntitySaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;

import java.sql.Connection;
import java.util.function.Consumer;

class SimpleEntitySaveCommandImpl<E>
        extends AbstractEntitySaveCommandImpl
        implements SimpleEntitySaveCommand<E> {

    private E entity;

    SimpleEntitySaveCommandImpl(
            SqlClient sqlClient,
            E entity
    ) {
        super(sqlClient, null);
        if (!(entity instanceof ImmutableSpi)) {
            throw new IllegalArgumentException("entity must be an immutable object");
        }
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

    @SuppressWarnings("unchecked")
    @Override
    public SimpleEntitySaveCommand<E> configure(Consumer<Cfg> block) {
        return (SimpleEntitySaveCommand<E>) super.configure(block);
    }

    @Override
    SimpleEntitySaveCommand<E> create(Data data) {
        return new SimpleEntitySaveCommandImpl<>(this, data);
    }
}
