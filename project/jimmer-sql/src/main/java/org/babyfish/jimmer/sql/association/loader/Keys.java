package org.babyfish.jimmer.sql.association.loader;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.meta.Column;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

class Keys {

    private Keys() {}

    public static Object key(ImmutableProp prop, ImmutableSpi source) {
        if (prop.getStorage() instanceof Column && source != null) {
            source = (ImmutableSpi) source.__get(prop.getName());
        }
        if (source == null) {
            return null;
        }
        return source.__get(source.__type().getIdProp().getName());
    }

    public static Map<ImmutableSpi, Object> keyMap(ImmutableProp prop, Collection<ImmutableSpi> sources) {
        Map<ImmutableSpi, Object> keyMap = new LinkedHashMap<>((sources.size() * 4 + 2) / 3);
        for (ImmutableSpi source : sources) {
            Object key = key(prop, source);
            if (key != null) {
                keyMap.put(source, key);
            }
        }
        return keyMap;
    }
}
