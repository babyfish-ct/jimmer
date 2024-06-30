package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

public interface LocatedCache<K, V> extends Cache<K, V> {

    ImmutableType getType();

    ImmutableProp getProp();

    interface Parameterized<K, V> extends LocatedCache<K, V>, Cache.Parameterized<K, V> {}
}
