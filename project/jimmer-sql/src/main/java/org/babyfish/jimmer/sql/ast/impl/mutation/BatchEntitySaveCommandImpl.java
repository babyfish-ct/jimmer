package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.impl.mutation.save.Saver2;
import org.babyfish.jimmer.sql.ast.mutation.AffectedTable;
import org.babyfish.jimmer.sql.ast.mutation.BatchEntitySaveCommand;
import org.babyfish.jimmer.sql.ast.mutation.BatchSaveResult;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.sql.Connection;
import java.util.*;
import java.util.function.Consumer;

public class BatchEntitySaveCommandImpl<E>
        extends AbstractEntitySaveCommandImpl
        implements BatchEntitySaveCommand<E> {

    private final Collection<E> entities;

    private final ImmutableType type;

    public BatchEntitySaveCommandImpl(
            JSqlClientImplementor sqlClient,
            Connection con,
            Collection<E> entities
    ) {
        super(sqlClient, con, null);
        ImmutableType type = null;
        for (E entity : entities) {
            if (!(entity instanceof ImmutableSpi)) {
                throw new IllegalArgumentException(
                        "All the elements of entities must be an immutable object"
                );
            }
            if (entity instanceof DraftSpi) {
                throw new IllegalArgumentException("Each element of entity cannot be a draft object");
            }
            ImmutableType entityType = ((ImmutableSpi) entity).__type();
            if (type != null && entityType != type) {
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
    public BatchSaveResult<E> execute(Connection con) {
        return sqlClient
                .getConnectionManager()
                .execute(con == null ? this.con : con, this::executeImpl);
    }

    @SuppressWarnings("unchecked")
    private BatchSaveResult<E> executeImpl(Connection con) {
        data.freeze();
        Saver2 saver = new Saver2(data, con, type);
        return saver.saveAll(entities);
    }

    @Override
    BatchEntitySaveCommand<E> create(Data data) {
        return new BatchEntitySaveCommandImpl<>(this, data);
    }
}
