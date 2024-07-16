package org.babyfish.jimmer.sql.cache.chain;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface Binder<K> {

    @Nullable ImmutableType type();

    @Nullable ImmutableProp prop();

    void deleteAll(Collection<K> keys, Object reason);
}
