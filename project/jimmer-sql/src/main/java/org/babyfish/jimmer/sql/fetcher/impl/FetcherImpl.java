package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.Dependency;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.ManyToManyView;
import org.babyfish.jimmer.sql.fetcher.FieldFilter;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.*;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;

import java.util.*;
import java.util.function.Consumer;

public class FetcherImpl<E> implements Fetcher<E> {

    final FetcherImpl<E> prev;

    private final ImmutableType immutableType;

    final boolean negative;

    final ImmutableProp prop;

    private final FieldFilter<?> filter;

    private final int batchSize;

    private final int limit;

    private final int offset;

    private final RecursionStrategy<?> recursionStrategy;

    final FetcherImpl<?> childFetcher;

    private Map<String, Field> fieldMap;

    private Boolean isSimpleFetcher;

    public FetcherImpl(Class<E> javaClass) {
        this(javaClass, null);
    }

    public FetcherImpl(Class<E> javaClass, FetcherImpl<E> base) {
        if (base != null) {
            if (base.getJavaClass() != javaClass) {
                throw new IllegalArgumentException(
                        "The owner type of base fetcher must be \"" +
                                javaClass.getName() +
                                "\""
                );
            }
            this.prev = base.prev;
            this.immutableType = base.immutableType;
            this.negative = base.negative;
            this.prop = base.prop;
            this.filter = base.filter;
            this.batchSize = base.batchSize;
            this.limit = base.limit;
            this.offset = base.offset;
            this.recursionStrategy = base.recursionStrategy;
            this.childFetcher = base.childFetcher;
        } else {
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
        if (negative || !prop.isAssociation(TargetLevel.PERSISTENT)) {
            this.childFetcher = null;
        } else {
            this.childFetcher = new FetcherImpl<>(prop.getTargetType().getJavaClass());
        }
    }

    @SuppressWarnings("unchecked")
    protected FetcherImpl(
            FetcherImpl<E> prev,
            ImmutableProp prop,
            FieldConfig<?, ? extends Table<?>> fieldConfig
    ) {
        this.prev = prev;
        this.immutableType = prev.immutableType;
        this.negative = false;
        this.prop = prop;
        if (fieldConfig != null) {
            FieldConfigImpl<?, Table<?>> loaderImpl = (FieldConfigImpl<?, Table<?>>) fieldConfig;
            this.filter = loaderImpl.getFilter();
            this.batchSize = loaderImpl.getBatchSize();
            this.limit = prop.isReferenceList(TargetLevel.PERSISTENT) ? loaderImpl.getLimit() : Integer.MAX_VALUE;
            this.offset = prop.isAssociation(TargetLevel.PERSISTENT) ? loaderImpl.getOffset() : 0;
            this.recursionStrategy = loaderImpl.getRecursionStrategy();
            this.childFetcher = standardChildFetcher(loaderImpl);
        } else {
            this.filter = null;
            this.batchSize = 0;
            this.limit = Integer.MAX_VALUE;
            this.offset = 0;
            this.recursionStrategy = null;
            this.childFetcher = null;
        }
    }

    FetcherImpl(
            FetcherImpl<E> prev,
            FetcherImpl<E> base,
            FetcherImpl<?> child
    ) {
        this.prev = prev;
        this.immutableType = base.immutableType;
        this.negative = base.negative;
        this.prop = base.prop;
        this.filter = base.filter;
        this.batchSize = base.batchSize;
        this.limit = base.limit;
        this.offset = base.offset;
        this.recursionStrategy = base.recursionStrategy;
        this.childFetcher = child;
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
    @SuppressWarnings("unchecked")
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
                                immutableType,
                                fetcher.prop,
                                fetcher.filter,
                                fetcher.batchSize,
                                fetcher.limit,
                                fetcher.offset,
                                fetcher.recursionStrategy,
                                fetcher.childFetcher,
                                false
                        );
                if (!map.containsKey(name)) {
                    map.putIfAbsent(name, field);
                    orderedNames.add(0, name);
                }
            }
            Map<String, Field> orderedMap = new LinkedHashMap<>();
            LinkedList<Field> extensionFields = new LinkedList<>();
            for (String name : orderedNames) {
                Field field = map.get(name);
                if (field != null) {
                    orderedMap.put(name, field);
                    ImmutableProp prop = field.getProp();
                    if (!prop.getDependencies().isEmpty()) {
                        extensionFields.add(field);
                    }
                }
            }
            while (!extensionFields.isEmpty()) {
                Field field = extensionFields.remove(0);
                for (Dependency dependency : field.getProp().getDependencies()) {
                    Field dependencyField = orderedMap.get(dependency.getProp().getName());
                    if (dependencyField == null) {
                        dependencyField = new FieldImpl(
                                immutableType,
                                dependency.getProp(),
                                dependency.getDeeperProp() != null && filter != null ?
                                        new MiddleEntityJoinFieldFilter(
                                                (FieldFilter<Table<?>>) field.getFilter(),
                                                dependency.getDeeperProp().getName()
                                        ) :
                                        null,
                                field.getBatchSize(),
                                field.getLimit(),
                                field.getOffset(),
                                null,
                                dependency.getDeeperProp() != null ?
                                        (FetcherImpl<?>) (new FetcherImpl<>((Class<Object>) dependency.getProp().getTargetType().getJavaClass())
                                                .add(
                                                        dependency.getDeeperProp().getName(),
                                                        field.getChildFetcher()
                                                )
                                        ) :
                                        null,
                                true
                        );
                        orderedMap.put(dependency.getProp().getName(), dependencyField);
                        if (prop.isFormula() && !(prop.getSqlTemplate() instanceof FormulaTemplate)) {
                            extensionFields.add(dependencyField);
                        }
                    } else if (dependency.getDeeperProp() != null) {
                        Fetcher<?> childFetcher = dependencyField.getChildFetcher();
                        String conflictCfgName = null;
                        if (dependencyField.getBatchSize() != field.getBatchSize()) {
                            conflictCfgName = "batchSize";
                        } else if (dependencyField.getLimit() != field.getLimit()) {
                            conflictCfgName = "limit";
                        } else if (dependencyField.getOffset() != field.getOffset()) {
                            conflictCfgName = "offset";
                        }
                        if (conflictCfgName != null) {
                            throw new IllegalArgumentException(
                                    "Both \"" +
                                            prop +
                                            "\" and \"" +
                                            dependency.getProp() +
                                            "\" are fetched, but the configuration \"" +
                                            conflictCfgName +
                                            "\" are not same"
                            );
                        }
                        if (dependencyField.getRecursionStrategy() != null || field.getRecursionStrategy() != null) {
                            throw new IllegalArgumentException(
                                    "Both \"" +
                                            prop +
                                            "\" and \"" +
                                            dependency.getProp() +
                                            "\" are fetched, so the recursion strategy cannot be specified"
                            );
                        }
                        if (childFetcher != null && childFetcher.getFieldMap().containsKey(dependency.getDeeperProp().getName())) {
                            try {
                                Fetcher<?> deeperFetcher = childFetcher
                                        .getFieldMap()
                                        .get(dependency.getDeeperProp().getName())
                                        .getChildFetcher();
                                childFetcher = childFetcher.add(
                                        dependency.getDeeperProp().getName(),
                                        new FetcherMergeContext().merge(
                                                field.getChildFetcher(),
                                                deeperFetcher
                                        )
                                );
                            } catch (FetcherMergeContext.ConflictException ex) {
                                throw new IllegalArgumentException(
                                        "Cannot merge the fetcher field \"" +
                                                field.getProp().getName() +
                                                ex.path +
                                                "\" and \"" +
                                                dependency.getProp().getName() +
                                                '.' +
                                                dependency.getDeeperProp().getName() +
                                                ex.path +
                                                "\", the configuration `" +
                                                ex.cfgName +
                                                "` is conflict"
                                );
                            }
                        } else {
                            if (childFetcher == null) {
                                childFetcher = new FetcherImpl<>(dependency.getProp().getTargetType().getJavaClass());
                            }
                            childFetcher = childFetcher.add(
                                    dependency.getDeeperProp().getName(),
                                    field.getChildFetcher()
                            );
                        }
                        dependencyField = new FieldImpl(
                                (FieldImpl) dependencyField,
                                (FetcherImpl<?>) childFetcher
                        );
                        orderedMap.put(dependency.getProp().getName(), dependencyField);
                    }
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
            if (!prop.isAssociation(TargetLevel.ENTITY) && !prop.isLogicalDeleted()) {
                fetcher = fetcher.addImpl(prop, null);
            }
        }
        return fetcher;
    }

    @NewChain
    @Override
    public Fetcher<E> add(String prop) {
        ImmutableProp immutableProp = immutableType.getProp(prop);
        return addImpl(immutableProp, false);
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
        return addImpl(immutableProp, true);
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
            Consumer<? extends FieldConfig<?, ? extends Table<?>>> loaderBlock
    ) {
        Objects.requireNonNull(prop, "'prop' cannot be null");
        ImmutableProp immutableProp = immutableType.getProp(prop);
        if (childFetcher != null && !immutableProp.isAssociation(TargetLevel.ENTITY)) {
            throw new IllegalArgumentException(
                    "Cannot load scalar property \"" +
                            immutableProp +
                            "\" with child fetcher"
            );
        }
        if (childFetcher != null && immutableProp.getTargetType().getJavaClass() != childFetcher.getJavaClass()) {
            throw new IllegalArgumentException("Illegal type of childFetcher");
        }
        FieldConfigImpl<Object, Table<Object>> loaderImpl = new FieldConfigImpl<>(immutableProp, (FetcherImpl<?>) childFetcher);
        if (loaderBlock != null) {
            ((Consumer<FieldConfig<Object, Table<Object>>>) loaderBlock).accept(loaderImpl);
            if (immutableProp.isRemote()) {
                if (loaderImpl.getFilter() != null) {
                    throw new IllegalArgumentException(
                            "Fetcher field based one \"" +
                                    immutableProp +
                                    "\" does not support `filter` because the association is remote"
                    );
                }
                if (loaderImpl.getLimit() != Integer.MAX_VALUE) {
                    throw new IllegalArgumentException(
                            "Fetcher field based one \"" +
                                    immutableProp +
                                    "\" does not support `limit` because the association is remote"
                    );
                }
                if (loaderImpl.getOffset() != 0) {
                    throw new IllegalArgumentException(
                            "Fetcher field based one \"" +
                                    immutableProp +
                                    "\" does not support `offset` because the association is remote"
                    );
                }
            }
            if (loaderImpl.getLimit() != Integer.MAX_VALUE && loaderImpl.getBatchSize() != 1) {
                throw new IllegalArgumentException(
                        "Fetcher field based on \"" +
                                immutableProp +
                                "\" with limit does not support batch load, " +
                                "the batchSize must be set to 1 when limit is set"
                );
            }
            if (immutableProp.getManyToManyViewBaseProp() != null &&
                    loaderImpl.getRecursionStrategy() != null) {
                throw new IllegalArgumentException(
                        "Fetcher field based on \"" +
                                immutableProp +
                                "\" does not support recursion strategy because it is decorated by @" +
                                ManyToManyView.class.getName()
                );
            }
        }
        return addImpl(immutableProp, loaderImpl);
    }

    @NewChain
    private FetcherImpl<E> addImpl(ImmutableProp prop, boolean negative) {
        if (prop.isId()) {
            return this;
        }
        if (prop.isTransient() && !prop.hasTransientResolver()) {
            throw new IllegalArgumentException(
                    "Cannot fetch \"" +
                            prop +
                            "\", it is transient property without resolver"
            );
        }
        return createFetcher(prop, negative);
    }

    @NewChain
    private FetcherImpl<E> addImpl(ImmutableProp prop, FieldConfigImpl<?, ? extends Table<?>> loader) {
        if (prop.isId()) {
            return this;
        }
        return createFetcher(prop, loader);
    }

    @Override
    public String toString() {
        return toString(false);
    }

    @Override
    public String toString(boolean multiLine) {
        FetcherWriter writer = new FetcherWriter(multiLine ? 4 : 0);
        writer.writeRoot(this);
        return writer.toString();
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

    protected FetcherImpl<E> createFetcher(ImmutableProp prop, boolean negative) {
        return new FetcherImpl<>(this, prop, negative);
    }

    protected FetcherImpl<E> createFetcher(ImmutableProp prop, FieldConfig<?, ? extends Table<?>> fieldConfig) {
        return new FetcherImpl<>(this, prop, fieldConfig);
    }

    private static FetcherImpl<?> standardChildFetcher(FieldConfigImpl<?, Table<?>> loaderImpl) {
        FetcherImpl<?> childFetcher = loaderImpl.getChildFetcher();
        if (!(loaderImpl.getProp().getStorage() instanceof ColumnDefinition)) {
            return childFetcher;
        }
        RecursionStrategy<?> strategy = loaderImpl.getRecursionStrategy();
        if (strategy == null) {
            return childFetcher;
        }
        if (strategy instanceof DefaultRecursionStrategy<?> &&
                ((DefaultRecursionStrategy<?>) strategy).getDepth() == 1) {
            return childFetcher;
        }
        if (childFetcher == null) {
            childFetcher = new FetcherImpl<>(loaderImpl.getProp().getElementClass());
        }
        childFetcher = (FetcherImpl<?>) childFetcher.add(loaderImpl.getProp().getName());
        return childFetcher;
    }
}
