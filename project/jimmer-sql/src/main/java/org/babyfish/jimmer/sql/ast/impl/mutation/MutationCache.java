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

    private final SqlClient sqlClientWithoutCache;

    private final Map<TypedId, ImmutableSpi> idObjMap = new HashMap<>();

    private final Map<TypedKey, ImmutableSpi> keyObjMap = new HashMap<>();

    public MutationCache(SqlClient sqlClient) {
        this.sqlClientWithoutCache = sqlClient.caches(null);
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

    public ImmutableSpi findById(ImmutableType type, Object id, Connection con) {
        TypedId typedId = new TypedId(type, id);
        ImmutableSpi spi = idObjMap.get(typedId);
        if (spi != null || idObjMap.containsKey(typedId)) {
            return spi;
        }
        spi = (ImmutableSpi) sqlClientWithoutCache
                .getEntities()
                .forUpdate()
                .forConnection(con)
                .findById(type.getJavaClass(), id);
        idObjMap.put(typedId, spi);
        return spi;
    }

    @SuppressWarnings("unchecked")
    public Map<Object, ImmutableSpi> findByIds(ImmutableType type, Collection<?> ids, Connection con) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Object, ImmutableSpi> resultMap = new LinkedHashMap<>((ids.size() * 4 + 2) / 3);
        for (Object id : ids) {
            TypedId typedId = new TypedId(type, id);
            ImmutableSpi spi = idObjMap.get(typedId);
            if (spi != null || idObjMap.containsKey(typedId)) {
                resultMap.put(id, spi);
            }
        }
        if (resultMap.size() < ids.size()) {
            List<Object> missedIds = new ArrayList<>(ids.size() - resultMap.size());
            for (Object id : ids) {
                if (!resultMap.containsKey(id)) {
                    missedIds.add(id);
                }
            }
            if (!missedIds.isEmpty()) { // "ids" is not Set
                Map<Object, ImmutableSpi> loadedMap =
                        sqlClientWithoutCache.getEntities().findMapByIds(
                                (Class<ImmutableSpi>) type.getJavaClass(),
                                missedIds
                        );
                for (Object id : missedIds) {
                    ImmutableSpi spi = loadedMap.get(id);
                    resultMap.put(id, spi);
                    idObjMap.put(new TypedId(type, id), spi);
                }
            }
        }
        return resultMap;
    }

    public ImmutableSpi save(ImmutableSpi spi) {
        return save(spi, true);
    }

    public ImmutableSpi save(ImmutableSpi spi, boolean merge) {

        ImmutableType type = spi.__type();
        ImmutableProp idProp = type.getIdProp();
        Set<ImmutableProp> keyProps = keyProps(type);

        if (merge) {

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

        return spi;
    }

    protected Set<ImmutableProp> keyProps(ImmutableType type) {
        return Collections.emptySet();
    }
}
