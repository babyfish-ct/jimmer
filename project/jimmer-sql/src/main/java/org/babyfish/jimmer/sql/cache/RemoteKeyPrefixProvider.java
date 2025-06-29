package org.babyfish.jimmer.sql.cache;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

public interface RemoteKeyPrefixProvider {

    RemoteKeyPrefixProvider DEFAULT = new RemoteKeyPrefixProvider() {};

    default String typeKeyPrefix(ImmutableType type) {
        return type.getJavaClass().getSimpleName() + '-';
    }

    default String propKeyPrefix(ImmutableProp prop) {
        return prop.getDeclaringType().getJavaClass().getSimpleName() + '.' + prop.getName() + '-';
    }
}
