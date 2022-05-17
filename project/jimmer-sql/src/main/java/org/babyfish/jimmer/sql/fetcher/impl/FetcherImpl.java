package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.Loader;
import org.babyfish.jimmer.sql.fetcher.spi.AbstractTypedFetcher;

import java.util.*;
import java.util.function.Consumer;

public class FetcherImpl<E> implements Fetcher<E> {

    private final FetcherImpl<E> prev;

    private final ImmutableType immutableType;

    private final boolean negative;

    private final ImmutableProp prop;

    private final int batchSize;

    private final int limit;

    private final int depth;

    private final Fetcher<?> childFetcher;

    private Map<String, Field> fieldMap;

    public FetcherImpl(Class<E> javaClass) {
        this.immutableType = ImmutableType.get(javaClass);
        this.negative = false;
        this.prop = immutableType.getIdProp();
        this.prev = null;
        this.batchSize = 0;
        this.limit = Integer.MAX_VALUE;
        this.depth = 1;
        this.childFetcher = null;
    }

    private FetcherImpl(
            FetcherImpl<E> prev,
            ImmutableProp prop,
            LoaderImpl loader
    ) {
        this.immutableType = prev.immutableType;
        this.negative = false;
        this.prop = prop;
        this.prev = prev;
        if (loader != null) {
            this.batchSize = loader.getBatchSize();
            this.limit = loader.getLimit();
            this.depth = loader.getDepth();
            this.childFetcher = loader.getChildFetcher();
        } else {
            this.batchSize = 0;
            this.limit = Integer.MAX_VALUE;
            this.depth = 1;
            this.childFetcher = null;
        }
    }

    private FetcherImpl(FetcherImpl<E> prev, ImmutableProp prop, boolean negative) {
        this.immutableType = prev.immutableType;
        this.prev = prev;
        this.negative = negative;
        this.prop = prop;
        this.batchSize = 0;
        this.limit = Integer.MAX_VALUE;
        this.depth = 1;
        this.childFetcher = null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<E> getJavaClass() {
        return (Class<E>) immutableType.getJavaClass();
    }

    @Override
    public ImmutableType getImmutableType() {
        return immutableType;
    }

    @Override
    public Map<String, Field> getFieldMap() {
        Map<String, Field> map = fieldMap;
        if (map == null) {
            map = new LinkedHashMap<>();
            for (FetcherImpl<E> fetcher = this; fetcher != null; fetcher = fetcher.prev) {
                String name = fetcher.prop.getName();
                Field field = fetcher.negative ?
                        null :
                        new FieldImpl(
                                fetcher.prop,
                                fetcher.batchSize,
                                fetcher.limit,
                                fetcher.depth,
                                unwrap(fetcher.childFetcher)
                        );
                if (!map.containsKey(name)) {
                    map.putIfAbsent(name, field);
                }
                map.values().removeIf(Objects::isNull);
            }
            map = Collections.unmodifiableMap(map);
            fieldMap = map;
        }
        return map;
    }

    @NewChain
    @Override
    public Fetcher<E> addSelectable() {
        FetcherImpl<E> fetcher = this;
        for (ImmutableProp prop : immutableType.getSelectableProps().values()) {
            fetcher = fetcher.addImpl(prop, null);
        }
        return fetcher;
    }

    @NewChain
    @Override
    public Fetcher<E> addScalars() {
        FetcherImpl<E> fetcher = this;
        for (ImmutableProp prop : immutableType.getSelectableProps().values()) {
            if (!prop.isAssociation()) {
                fetcher = fetcher.addImpl(prop, null);
            }
        }
        return fetcher;
    }

    @NewChain
    @Override
    public Fetcher<E> add(String prop) {
        ImmutableProp immutableProp = immutableType.getProp(prop);
        return addImpl(immutableProp, null);
    }

    @NewChain
    @Override
    public Fetcher<E> remove(String prop) {
        ImmutableProp immutableProp = immutableType.getProp(prop);
        if (immutableProp.isId()) {
            throw new IllegalArgumentException(
                    "Id property \"" +
                            immutableProp +
                            "\" cannot be removed"
            );
        }
        return new FetcherImpl<>(this, immutableProp, true);
    }

    @NewChain
    @Override
    public Fetcher<E> add(String prop, Fetcher<?> childFetcher) {
        return add(prop, null, childFetcher);
    }

    @NewChain
    @SuppressWarnings("unchecked")
    @Override
    public Fetcher<E> add(String prop, Consumer<? extends Loader> loaderBlock, Fetcher<?> childFetcher) {
        Objects.requireNonNull(prop, "'prop' cannot be null");
        Objects.requireNonNull(childFetcher, "'childFetcher' cannot be null");
        ImmutableProp immutableProp = immutableType.getProp(prop);
        if (!immutableProp.isAssociation()) {
            throw new IllegalArgumentException(
                    "Cannot load scalar property \"" +
                            immutableProp +
                            "\", please call get function"
            );
        }
        LoaderImpl loaderImpl;
        if (loaderBlock != null) {
            ImmutableType targetType = immutableProp.getTargetType();
            loaderImpl = targetType != null &&
                    immutableProp
                            .getDeclaringType()
                            .getJavaClass()
                            .isAssignableFrom(targetType.getJavaClass()) ?
                    new RecursiveLoaderImpl(childFetcher) :
                    new LoaderImpl(childFetcher);
            ((Consumer<Loader>)loaderBlock).accept(loaderImpl);
        } else {
            loaderImpl = new LoaderImpl(childFetcher);
        }
        return addImpl(immutableProp, loaderImpl);
    }

    @NewChain
    private FetcherImpl<E> addImpl(ImmutableProp prop, LoaderImpl loader) {
        if (prop.isId()) {
            return this;
        }
        return new FetcherImpl<>(this, prop, loader);
    }

    @Override
    public String toString() {
        return toString(true);
    }

    String toString(boolean includeTypeName) {
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        if (includeTypeName) {
            joiner.add("$type: " + getImmutableType());
        }
        for (Field field : getFieldMap().values()) {
            joiner.add(field.toString());
        }
        return joiner.toString();
    }

    static <E> FetcherImpl<E> unwrap(Fetcher<E> fetcher) {
        if (fetcher instanceof AbstractTypedFetcher<?>) {
            return (FetcherImpl<E>)((AbstractTypedFetcher<E>) fetcher).__unwrap();
        }
        return (FetcherImpl<E>) fetcher;
    }
}
