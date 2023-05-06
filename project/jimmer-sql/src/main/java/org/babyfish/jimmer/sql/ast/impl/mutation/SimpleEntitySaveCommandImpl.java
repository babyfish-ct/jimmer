package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.mutation.SimpleEntitySaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.sql.Connection;
import java.util.function.Consumer;

public class SimpleEntitySaveCommandImpl<E>
        extends AbstractEntitySaveCommandImpl
        implements SimpleEntitySaveCommand<E> {

    private final E entity;

    public SimpleEntitySaveCommandImpl(
            JSqlClientImplementor sqlClient,
            Connection con,
            E entity
    ) {
        super(sqlClient, con, null);
        if (!(entity instanceof ImmutableSpi)) {
            throw new IllegalArgumentException("entity must be an immutable object");
        }
        this.entity = entity;
    }

    private SimpleEntitySaveCommandImpl(
            SimpleEntitySaveCommandImpl<E> base,
            Data data
    ) {
        super(base.sqlClient, base.con, data);
        this.entity = base.entity;
    }

    @Override
    public SimpleSaveResult<E> execute() {
        if (con != null) {
            return executeImpl(con);
        }
        return sqlClient
                .getConnectionManager()
                .execute(this::executeImpl);
    }

    @Override
    public SimpleSaveResult<E> execute(Connection con) {
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

    private SimpleSaveResult<E> executeImpl(Connection con) {
        Saver saver = new Saver(data, con, ((ImmutableSpi)entity).__type());
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
