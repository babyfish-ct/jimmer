package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.sql.Column;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.ast.mutation.SimpleSaveResult;
import org.babyfish.jimmer.sql.runtime.ExecutionException;

import java.sql.Connection;
import java.util.*;

public class Saver {

    private AbstractSaveCommandImpl.Data data;

    private Connection con;

    private Map<String, Integer> affectedRowCount;

    private ImmutableCache cache;

    public Saver(
            AbstractSaveCommandImpl.Data data,
            Connection con,
            ImmutableCache cache) {
        this.data = data;
        this.con = con;
        this.cache = cache;
    }

    @SuppressWarnings("unchecked")
    public <E> SimpleSaveResult<E> save(E entity) {
        ImmutableType immutableType = ImmutableType.get(entity.getClass());
        E newEntity = (E)Internal.produce(immutableType, entity, draft -> {
            saveImpl((DraftSpi) draft);
        });
        return null;
    }

    private void saveImpl(DraftSpi draftSpi) {

    }

    private void saveParents(DraftSpi draftSpi) {
        for (ImmutableProp prop : draftSpi.__type().getProps().values()) {
            Object parent = draftSpi.__get(prop.getName());
            if (parent != null &&
                    prop.isReference() &&
                    prop.getStorage() instanceof Column &&
                    data.getAutoAttachingSet().contains(prop)
            ) {

            }
        }
    }

    @SuppressWarnings("unchecked")
    private Object findAssociatedObject(ImmutableProp prop, DraftSpi example) {

        ImmutableSpi cached = cache.find(example);
        if (cached != null) {
            return cached;
        }

        ImmutableType targetType = prop.getTargetType();
        ImmutableProp idProp = targetType.getIdProp();
        Object id = example.__isLoaded(idProp.getName()) ?
                example.__get(idProp.getName()) :
                null;
        Collection<ImmutableProp> actualKeyProps;
        if (id != null) {
            actualKeyProps = Collections.singleton(idProp);
        } else {
            Set<ImmutableProp> keyProps = data.getKeyPropMultiMap().get(targetType);
            if (keyProps == null) {
                throw new ExecutionException(
                        "Cannot save \"" + targetType + "\" without id, " +
                                "key properties is not configured"
                );
            }
            actualKeyProps = keyProps;
        }

        List<ImmutableSpi> rows = (List<ImmutableSpi>)Queries.createQuery(data.getSqlClient(), targetType, (q, table) -> {
            for (ImmutableProp keyProp : actualKeyProps) {
                q.where(
                        table.<Expression<Object>>get(keyProp.getName()).eq(
                                example.__get(keyProp.getName())
                        )
                );
            }
            return q.select(table);
        }).execute(con);

        ImmutableSpi spi = rows.isEmpty() ? null : rows.get(0);
        if (spi != null) {
            cache.save(spi);
        }
        return spi;
    }
}
