package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.ast.query.Filterable;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.*;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FetcherImpl<E> implements Fetcher<E> {

    private final FetcherImpl<E> prev;

    private final ImmutableType immutableType;

    private final boolean negative;

    private final ImmutableProp prop;

    private final Filter<E, ?> filter;

    private final int batchSize;

    private final int limit;

    private final int offset;

    private final RecursionStrategy<?> recursionStrategy;

    private final FetcherImpl<?> childFetcher;

    private Map<String, Field> fieldMap;

    private Boolean isSimpleFetcher;

    public FetcherImpl(Class<E> javaClass) {
        this.prev = null;
        this.immutableType = ImmutableType.get(javaClass);
        this.negative = false;
        this.prop = immutableType.getIdProp();
        this.filter = null;
        this.batchSize = 0;
        this.limit = Integer.MAX_VALUE;
        this.offset = 0;
        this.recursionStrategy = null;
        this.childFetcher = null;
    }

    protected FetcherImpl(FetcherImpl<E> prev, ImmutableProp prop, boolean negative) {
        this.prev = prev;
        this.immutableType = prev.immutableType;
        this.negative = negative;
        this.prop = prop;
        this.filter = null;
        this.batchSize = 0;
        this.limit = Integer.MAX_VALUE;
        this.offset = 0;
        this.recursionStrategy = null;
        this.childFetcher = null;
    }

    @SuppressWarnings("unchecked")
    protected FetcherImpl(
            FetcherImpl<E> prev,
            ImmutableProp prop,
            Loader<?, Table<?>> loader
    ) {
        this.prev = prev;
        this.immutableType = prev.immutableType;
        this.negative = false;
        this.prop = prop;
        if (loader != null) {
            LoaderImpl<?, Table<?>> loaderImpl = (LoaderImpl<?, Table<?>>) loader;
            this.filter = (Filter<E, ?>) loaderImpl.getFilter();
            this.batchSize = loaderImpl.getBatchSize();
            this.limit = prop.isEntityList() ? loaderImpl.getLimit() : Integer.MAX_VALUE;
            this.offset = prop.isAssociation() ? loaderImpl.getOffset() : 0;
            this.recursionStrategy = loaderImpl.getRecursionStrategy();
            this.childFetcher = loaderImpl.getChildFetcher();
        } else {
            this.filter = null;
            this.batchSize = 0;
            this.limit = Integer.MAX_VALUE;
            this.offset = 0;
            this.recursionStrategy = null;
            this.childFetcher = null;
        }
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
            map = new HashMap<>();
            LinkedList<String> orderedNames = new LinkedList<>();
            for (FetcherImpl<E> fetcher = this; fetcher != null; fetcher = fetcher.prev) {
                String name = fetcher.prop.getName();
                Field field = fetcher.negative ?
                        null :
                        new FieldImpl(
                                fetcher.prop,
                                fetcher.filter,
                                fetcher.batchSize,
                                fetcher.limit,
                                fetcher.offset,
                                fetcher.recursionStrategy,
                                fetcher.childFetcher
                        );
                if (!map.containsKey(name)) {
                    map.putIfAbsent(name, field);
                    orderedNames.add(0, name);
                }
            }
            Map<String, Field> orderedMap = new LinkedHashMap<>();
            for (String name : orderedNames) {
                Field field = map.get(name);
                if (field != null) {
                    orderedMap.put(name, field);
                }
            }
            map = Collections.unmodifiableMap(orderedMap);
            fieldMap = map;
        }
        return map;
    }

    @NewChain
    @Override
    public Fetcher<E> allTableFields() {
        FetcherImpl<E> fetcher = this;
        for (ImmutableProp prop : immutableType.getSelectableProps().values()) {
            fetcher = fetcher.addImpl(prop, null);
        }
        return fetcher;
    }

    @NewChain
    @Override
    public Fetcher<E> allScalarFields() {
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
        return createChildFetcher(immutableProp, true);
    }

    @NewChain
    @Override
    public Fetcher<E> add(String prop, Fetcher<?> childFetcher) {
        return add(prop, childFetcher, null);
    }

    @NewChain
    @SuppressWarnings("unchecked")
    @Override
    public Fetcher<E> add(
            String prop,
            Fetcher<?> childFetcher,
            Consumer<? extends Loader<?, ? extends Table<?>>> loaderBlock
    ) {
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
        if (immutableProp.getTargetType().getJavaClass() != childFetcher.getJavaClass()) {
            throw new IllegalArgumentException("Illegal type of childFetcher");
        }
        LoaderImpl<Object, Table<Object>> loaderImpl = new LoaderImpl<>(immutableProp, (FetcherImpl<?>) childFetcher);
        if (loaderBlock != null) {
            ((Consumer<Loader<Object, Table<Object>>>)loaderBlock).accept(loaderImpl);
            if (loaderImpl.getLimit() != Integer.MAX_VALUE && loaderImpl.getBatchSize() != 1) {
                throw new IllegalArgumentException(
                        "Fetcher field with limit does not support batch load, " +
                                "the batchSize must be set to 1 when limit is set"
                );
            }
        }
        return addImpl(immutableProp, loaderImpl);
    }

    @NewChain
    private FetcherImpl<E> addImpl(ImmutableProp prop, LoaderImpl<?, ?> loader) {
        if (prop.isId()) {
            return this;
        }
        return createChildFetcher(prop, loader);
    }

    @Override
    public String toString() {
        return toString(true);
    }

    String toString(boolean includeTypeName) {
        StringJoiner joiner = new StringJoiner(", ", " { ", " }");
        for (Field field : getFieldMap().values()) {
            joiner.add(field.toString());
        }
        if (includeTypeName) {
            return getJavaClass().getName() + joiner.toString();
        }
        return joiner.toString();
    }

    @Override
    public boolean isSimpleFetcher() {
        Boolean isSimple = isSimpleFetcher;
        if (isSimple == null) {
            isSimple = true;
            for (Field field : getFieldMap().values()) {
                if (!field.isSimpleField()) {
                    isSimple = false;
                    break;
                }
            }
            isSimpleFetcher = isSimple;
        }
        return isSimple;
    }

    protected FetcherImpl<E> createChildFetcher(ImmutableProp prop, boolean negative) {
        return new FetcherImpl<>(this, prop, negative);
    }

    protected FetcherImpl<E> createChildFetcher(ImmutableProp prop, Loader loader) {
        return new FetcherImpl<>(this, prop, loader);
    }
}
