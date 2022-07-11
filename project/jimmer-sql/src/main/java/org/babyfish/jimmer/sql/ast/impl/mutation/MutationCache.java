package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.SqlClient;

import java.sql.Connection;
import java.util.*;

class MutationCache {

    private final SqlClient sqlClient;

    private final Map<TypedId, ImmutableSpi> idObjMap = new HashMap<>();

    private final Map<TypedKey, ImmutableSpi> keyObjMap = new HashMap<>();

    public MutationCache(SqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    public ImmutableSpi find(ImmutableSpi example) {
        ImmutableType type = example.__type();
        ImmutableProp idProp = type.getIdProp();
        if (example.__isLoaded(idProp.getName())) {
            Object id = example.__get(idProp.getName());
            if (id != null) {
                return idObjMap.get(new TypedId(type, id));
            }
        }
        TypedKey key = TypedKey.of(example, keyProps(type), true);
        return keyObjMap.get(key);
    }

    public ImmutableSpi getById(Class<?> type, Object id, Connection con) {
        TypedId typedId = new TypedId(ImmutableType.get(type), id);
        ImmutableSpi spi = idObjMap.get(typedId);
        if (spi != null || idObjMap.containsKey(typedId)) {
            return spi;
        }
        spi = (ImmutableSpi) sqlClient.getEntities().findById(type, id, con);
        idObjMap.put(typedId, spi);
        return spi;
    }

    public void save(ImmutableSpi spi) {
        save(spi, false);
    }

    public void save(ImmutableSpi spi, boolean insertOnly) {

        ImmutableType type = spi.__type();
        ImmutableProp idProp = type.getIdProp();
        Set<ImmutableProp> keyProps = keyProps(type);

        if (!insertOnly) {

            ImmutableSpi oldSpi = find(spi);
            if (oldSpi != null) {

                TypedId oldTypedId = new TypedId(type, oldSpi.__get(idProp.getName()));
                idObjMap.remove(oldTypedId);

                if (keyProps != null && !keyProps.isEmpty()) {
                    TypedKey oldKey = TypedKey.of(oldSpi, keyProps, false);
                    if (oldKey != null) {
                        keyObjMap.remove(oldKey);
                    }
                }

                ImmutableSpi newSpi = spi;
                spi = (ImmutableSpi) Internal.produce(spi.__type(), oldSpi, draft -> {
                    for (ImmutableProp prop : type.getProps().values()) {
                        if (newSpi.__isLoaded(prop.getName())) {
                            ((DraftSpi) draft).__set(prop.getName(), newSpi.__get(prop.getName()));
                        }
                    }
                });
            }
        }

        TypedId typedId = new TypedId(type, spi.__get(idProp.getName()));
        idObjMap.put(typedId, spi);

        if (keyProps != null && !keyProps.isEmpty()) {
            TypedKey key = TypedKey.of(spi, keyProps, false);
            if (key != null) {
                keyObjMap.put(key, spi);
            }
        }
    }

    protected Set<ImmutableProp> keyProps(ImmutableType type) {
        return Collections.emptySet();
    }
}
