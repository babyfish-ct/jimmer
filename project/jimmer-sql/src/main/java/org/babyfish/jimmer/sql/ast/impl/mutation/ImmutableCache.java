package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;

import java.util.HashMap;
import java.util.Map;

class ImmutableCache {

    private AbstractSaveCommandImpl.Data data;

    private Map<Object, ImmutableSpi> idObjMap = new HashMap<>();

    private Map<Key, ImmutableSpi> keyObjMap = new HashMap<>();

    public ImmutableCache(AbstractSaveCommandImpl.Data data) {
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
        ImmutableType type = spi.__type();
        ImmutableProp idProp = type.getIdProp();
        Object id = spi.__get(idProp.getName());
        idObjMap.put(id, spi);
        Key key = Key.of(data, spi, false);
        if (key != null) {
            keyObjMap.put(key, spi);
        }
    }
}
