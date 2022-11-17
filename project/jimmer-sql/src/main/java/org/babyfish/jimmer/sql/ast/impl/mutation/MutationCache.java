package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.cache.CacheDisableConfig;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;

import java.sql.Connection;
import java.util.*;

class MutationCache {

    private final JSqlClient sqlClientWithoutCache;

    private final Map<TypedId, ImmutableSpi> idObjMap = new HashMap<>();

    private final Map<TypedKey, ImmutableSpi> keyObjMap = new HashMap<>();

    private final IdentityHashMap<Object, Object> savedMap = new IdentityHashMap<>();

    public MutationCache(JSqlClient sqlClient) {
        this.sqlClientWithoutCache = sqlClient.caches(CacheDisableConfig::disableAll);
    }

    public ImmutableSpi find(ImmutableSpi example) {
        ImmutableType type = example.__type();
        ImmutableProp idProp = type.getIdProp();
        int idPropId = idProp.getId();
        if (example.__isLoaded(idPropId)) {
            Object id = example.__get(idPropId);
            if (id != null) {
                return idObjMap.get(new TypedId(type, id));
            }
        }
        TypedKey key = TypedKey.of(example, keyProps(type), true);
        return keyObjMap.get(key);
    }

    @SuppressWarnings("unchecked")
    public List<ImmutableSpi> loadByIds(ImmutableType type, Collection<Object> ids, Connection con) {
        if (!(ids instanceof Set<?>)) {
            ids = new HashSet<>(ids);
        }
        List<ImmutableSpi> list = new ArrayList<>(ids.size());
        Collection<Object> missedIds = new ArrayList<>();
        for (Object id : ids) {
            ImmutableSpi spi = idObjMap.get(new TypedId(type, id));
            if (spi != null) {
                list.add(spi);
            } else {
                missedIds.add(id);
            }
        }
        if (!missedIds.isEmpty()) {
            String idPropName = type.getIdProp().getName();
            List<ImmutableSpi> rows = Internal.requiresNewDraftContext(ctx -> {
                List<ImmutableSpi> spiList = (List<ImmutableSpi>)
                        Queries.createQuery(sqlClientWithoutCache, type, ExecutionPurpose.MUTATE, true, (q, t) -> {
                            q.where(t.<Expression<Object>>get(idPropName).in(missedIds));
                            return q.select(t);
                        }).forUpdate().execute(con);
                return ctx.resolveList(spiList);
            });
            for (ImmutableSpi row : rows) {
                save(row, false);
                list.add(row);
            }
        }
        return list;
    }

    public ImmutableSpi save(ImmutableSpi spi, boolean forUserSave) {

        ImmutableType type = spi.__type();
        ImmutableProp idProp = type.getIdProp();
        Set<ImmutableProp> keyProps = keyProps(type);

        ImmutableSpi oldSpi = find(spi);
        if (oldSpi != null) {

            TypedId oldTypedId = new TypedId(type, oldSpi.__get(idProp.getId()));
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
                    int propId = prop.getId();
                    if (newSpi.__isLoaded(propId)) {
                        ((DraftSpi) draft).__set(propId, newSpi.__get(prop.getId()));
                    }
                }
            });
        }

        TypedId typedId = new TypedId(type, spi.__get(idProp.getId()));
        idObjMap.put(typedId, spi);

        if (keyProps != null && !keyProps.isEmpty()) {
            TypedKey key = TypedKey.of(spi, keyProps, false);
            if (key != null) {
                keyObjMap.put(key, spi);
            }
        }

        if (forUserSave) {
            savedMap.put(spi, null);
        }

        return spi;
    }

    public boolean isSaved(ImmutableSpi spi) {
        return savedMap.containsKey(spi);
    }

    protected Set<ImmutableProp> keyProps(ImmutableType type) {
        return Collections.emptySet();
    }
}
