package org.babyfish.jimmer.sql.cache.spi;

import org.babyfish.jimmer.impl.util.StringUtil;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.CacheTracker;
import org.babyfish.jimmer.sql.cache.chain.Binder;
import org.checkerframework.checker.units.qual.C;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

public abstract class AbstractBinder<K> implements Binder<K> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBinder.class);

    protected final ImmutableType type;

    protected final ImmutableProp prop;

    public AbstractBinder(
            @Nullable ImmutableType type,
            @Nullable ImmutableProp prop
    ) {
        if ((type == null) == (prop == null)) {
            throw new IllegalArgumentException("The nullity of type and prop must be different");
        }
        if (prop == null) {
            this.type = type;
            this.prop = null;
        } else {
            this.type = null;
            this.prop = prop;
        }
    }

    @Override
    public final @Nullable ImmutableType type() {
        return type;
    }

    @Override
    public final @Nullable ImmutableProp prop() {
        return prop;
    }

    @Override
    public final void deleteAll(Collection<K> keys, Object reason) {
        if (reason == null || matched(reason)) {
            if (LOGGER.isInfoEnabled()) {
                logDeletedKeys(keys);
            }
            deleteAllImpl(keys);
        }
    }

    protected abstract void deleteAllImpl(Collection<K> keys);

    protected void logDeletedKeys(Collection<?> keys) {
        LOGGER.info(
                "Cache.Delete > {}: {}",
                toString(true),
                keys.size() == 1 ?
                        keys.iterator().next() :
                        keys
        );
    }

    protected abstract boolean matched(@Nullable Object reason);

    @Override
    public String toString() {
        return toString(false);
    }

    protected String toString(boolean simpleName) {
        String metadata;
        if (prop != null) {
            if (simpleName) {
                metadata = prop.getDeclaringType().getJavaClass().getSimpleName() +
                        '.' +
                        prop.getName();
            } else {
                metadata = prop.toString();
            }
        } else if (type != null) {
            if (simpleName) {
                metadata = type.getJavaClass().getSimpleName();
            } else {
                metadata = type.toString();
            }
        } else {
            throw new AssertionError("Internal bug");
        }
        return StringUtil.removeSuffixes(
                this.getClass().getSimpleName(),
                "ValueBinder",
                "HashBinder",
                "Binder"
        ) + '(' + metadata + ')';
    }
}
