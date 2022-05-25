package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.runtime.Internal;

import java.util.HashMap;
import java.util.Map;

class ImmutableCache {

    private AbstractEntitySaveCommandImpl.Data data;

    private Map<Object, ImmutableSpi> idObjMap = new HashMap<>();

    private Map<Key, ImmutableSpi> keyObjMap = new HashMap<>();

    public ImmutableCache(AbstractEntitySaveCommandImpl.Data data) {
        this.data = data;
    }

    public ImmutableSpi find(ImmutableSpi example) {
        ImmutableType type = example.__type();
        ImmutableProp idProp = type.getIdProp();
        if (example.__isLoaded(idProp.getName())) {
            Object id = example.__get(idProp.getName());
            if (id != null) {
                return idObjMap.get(id);
            }
        }
        Key key = Key.of(data, example, true);
        return keyObjMap.get(key);
    }

    public void save(ImmutableSpi spi) {
        save(spi, false);
    }

    public void save(ImmutableSpi spi, boolean insertOnly) {

        ImmutableType type = spi.__type();
        ImmutableProp idProp = type.getIdProp();

        if (!insertOnly) {

            ImmutableSpi oldSpi = find(spi);
            if (oldSpi != null) {

                Object oldId = oldSpi.__get(idProp.getName());
                idObjMap.remove(oldId);

                Key oldKey = Key.of(data, oldSpi, false);
                if (oldKey != null) {
                    keyObjMap.remove(oldKey);
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

        Object id = spi.__get(idProp.getName());
        idObjMap.put(id, spi);

        Key key = Key.of(data, spi, false);
        if (key != null) {
            keyObjMap.put(key, spi);
        }
    }
}
