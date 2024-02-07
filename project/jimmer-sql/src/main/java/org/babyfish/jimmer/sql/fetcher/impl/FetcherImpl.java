package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.sql.ManyToManyView;
import org.babyfish.jimmer.sql.fetcher.FieldFilter;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.*;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;

import java.util.*;
import java.util.function.Consumer;

public class FetcherImpl<E> implements FetcherImplementor<E> {

    final FetcherImpl<E> prev;

    private final ImmutableType immutableType;

    final boolean negative;

    final boolean rawId;

    final ImmutableProp prop;

    private final FieldFilter<?> filter;

    private final int batchSize;

    private final int limit;

    private final int offset;

    private final RecursionStrategy<?> recursionStrategy;

    final FetcherImpl<?> childFetcher;

    private Map<String, Field> fieldMap;

    private Map<String, Field> unresolvedFieldMap;

    private List<PropId> shownPropIds;

    private List<PropId> hiddenPropIds;

    private Boolean isSimpleFetcher;

    private transient int hash;

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
            this.rawId = base.rawId;
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
            this.rawId = false;
            this.prop = immutableType.getIdProp();
            this.filter = null;
            this.batchSize = 0;
            this.limit = Integer.MAX_VALUE;
            this.offset = 0;
            this.recursionStrategy = null;
            this.childFetcher = null;
        }
    }

    protected FetcherImpl(FetcherImpl<E> prev, ImmutableProp prop, boolean negative, IdOnlyFetchType idOnlyFetchType) {
        this.prev = prev;
        this.immutableType = prev.immutableType;
        this.negative = negative;
        this.rawId = idOnlyFetchType == IdOnlyFetchType.RAW;
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
        this.rawId = false;
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
        this.rawId = base.rawId;
        this.prop = base.prop;
        this.filter = base.filter;
        this.batchSize = base.batchSize;
        this.limit = base.limit;
        this.offset = base.offset;
        this.recursionStrategy = child != null ? base.recursionStrategy : null;
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
                Field field;
                String name = fetcher.prop.getName();
                if (fetcher.negative) {
                    field = null;
                } else {
                    field = new FieldImpl(
                            immutableType,
                            fetcher.prop,
                            fetcher.filter,
                            fetcher.batchSize,
                            fetcher.limit,
                            fetcher.offset,
                            fetcher.recursionStrategy,
                            fetcher.recursionStrategy == null ?
                                fetcher.childFetcher :
                                this.realRecursiveChild(fetcher),
                            false,
                            fetcher.rawId
                    );
                }
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
                                true,
                                false
                        );
                        orderedMap.put(dependency.getProp().getName(), dependencyField);
                        if (prop.isFormula() && !(prop.getSqlTemplate() instanceof FormulaTemplate)) {
                            extensionFields.add(dependencyField);
                        }
                    } else if (dependency.getDeeperProp() != null) {
                        FetcherImplementor<?> childFetcher = (FetcherImplementor<?>) dependencyField.getChildFetcher();
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

    @Override
    public Map<String, Field> __unresolvedFieldMap() {
        Map<String, Field> map = unresolvedFieldMap;
        if (map == null) {
            map = new LinkedHashMap<>();
            for (Map.Entry<String, Field> e : getFieldMap().entrySet()) {
                Field field = e.getValue();
                if (field.getProp().getDependencies().isEmpty() && (
                        !field.isSimpleField() || field.getProp().getTargetType() != null)
                ) {
                    map.put(e.getKey(), field);
                }
            }
            if (map.isEmpty()) {
                map = Collections.emptyMap();
            } else {
                map = Collections.unmodifiableMap(map);
            }
            unresolvedFieldMap = map;
        }
        return map;
    }

    @Override
    public List<PropId> __shownPropIds() {
        List<PropId> list = shownPropIds;
        if (list == null) {
            list = new ArrayList<>();
            for (Field field : getFieldMap().values()) {
                ImmutableProp prop = field.getProp();
                if (!prop.getDependencies().isEmpty()) {
                    list.add(prop.getId());
                }
            }
            if (list.isEmpty()) {
                list = Collections.emptyList();
            } else {
                list = Collections.unmodifiableList(list);
            }
            shownPropIds = list;
        }
        return list;
    }

    @Override
    public List<PropId> __hiddenPropIds() {
        List<PropId> list = hiddenPropIds;
        if (list == null) {
            list = new ArrayList<>();
            for (Field field : getFieldMap().values()) {
                if (field.isImplicit()) {
                    list.add(field.getProp().getId());
                }
            }
            if (list.isEmpty()) {
                list = Collections.emptyList();
            } else {
                list = Collections.unmodifiableList(list);
            }
            hiddenPropIds = list;
        }
        return list;
    }

    @NewChain
    @Override
    public FetcherImplementor<E> allTableFields() {
        FetcherImpl<E> fetcher = this;
        for (ImmutableProp prop : immutableType.getSelectableProps().values()) {
            ImmutableProp idViewProp = prop.getIdViewProp();
            fetcher = fetcher.addImpl(idViewProp != null ? idViewProp : prop, null);
        }
        return fetcher;
    }

    @NewChain
    @Override
    public FetcherImplementor<E> allScalarFields() {
        FetcherImpl<E> fetcher = this;
        for (ImmutableProp prop : immutableType.getSelectableScalarProps().values()) {
            fetcher = fetcher.addImpl(prop, null);
        }
        return fetcher;
    }

    @Override
    public Fetcher<E> allReferenceIds() {
        FetcherImpl<E> fetcher = this;
        for (ImmutableProp prop : immutableType.getReferenceProps().values()) {
            ImmutableProp idViewProp = prop.getIdViewProp();
            fetcher = fetcher.addImpl(idViewProp != null ? idViewProp : prop, null);
        }
        return fetcher;
    }

    @NewChain
    @Override
    public FetcherImplementor<E> add(String prop) {
        ImmutableProp immutableProp = immutableType.getProp(prop);
        return addImpl(immutableProp, false, IdOnlyFetchType.DEFAULT);
    }

    @NewChain
    @Override
    public FetcherImplementor<E> remove(String prop) {
        ImmutableProp immutableProp = immutableType.getProp(prop);
        if (immutableProp.isId()) {
            throw new IllegalArgumentException(
                    "Id property \"" +
                            immutableProp +
                            "\" cannot be removed"
            );
        }
        return addImpl(immutableProp, true, IdOnlyFetchType.DEFAULT);
    }

    @NewChain
    @Override
    public FetcherImplementor<E> add(String prop, Fetcher<?> childFetcher) {
        return add(prop, childFetcher, null);
    }

    @NewChain
    @SuppressWarnings("unchecked")
    @Override
    public FetcherImplementor<E> add(
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
            validateConfig(immutableProp, loaderImpl);
            if (loaderImpl.getRecursionStrategy() != null) {
                validateRecursiveProp(immutableProp);
                if (childFetcher != null) {
                    throw new IllegalArgumentException(
                            "Fetcher field based on \"" +
                                    immutableProp +
                                    "\" cannot have child fetcher because itself is recursive field"
                    );
                }
            }
        }
        return addImpl(immutableProp, loaderImpl);
    }

    @Override
    @NewChain
    @SuppressWarnings("unchecked")
    public FetcherImplementor<E> addRecursion(
            String prop,
            Consumer<? extends FieldConfig<?, ? extends Table<?>>> loaderBlock
    ) {
        Objects.requireNonNull(prop, "'prop' cannot be null");
        ImmutableProp immutableProp = immutableType.getProp(prop);
        validateRecursiveProp(immutableProp);
        FieldConfigImpl<Object, Table<Object>> loaderImpl = new FieldConfigImpl<>(immutableProp, null);
        if (loaderBlock != null) {
            ((Consumer<FieldConfig<Object, Table<Object>>>) loaderBlock).accept(loaderImpl);
            validateConfig(immutableProp, loaderImpl);
        }
        if (loaderImpl.getRecursionStrategy() == null) {
            loaderImpl.recursive(DefaultRecursionStrategy.of(Integer.MAX_VALUE));
        }
        return addImpl(immutableProp, loaderImpl);
    }

    @Override
    public FetcherImplementor<E> add(String prop, IdOnlyFetchType idOnlyFetchType) {
        Objects.requireNonNull(prop, "'prop' cannot be null");
        ImmutableProp immutableProp = immutableType.getProp(prop);
        ImmutableProp associationProp = immutableProp.getIdViewBaseProp();
        if (associationProp == null) {
            associationProp = immutableProp;
        }
        if (!associationProp.isAssociation(TargetLevel.PERSISTENT) || associationProp.getMappedBy() != null) {
            idOnlyFetchType = IdOnlyFetchType.DEFAULT;
        }
        return addImpl(immutableProp, false, idOnlyFetchType);
    }

    @NewChain
    private FetcherImpl<E> addImpl(ImmutableProp prop, boolean negative, IdOnlyFetchType idOnlyFetchType) {
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
        return createFetcher(prop, negative, idOnlyFetchType);
    }

    @NewChain
    private FetcherImpl<E> addImpl(ImmutableProp prop, FieldConfigImpl<?, ? extends Table<?>> loader) {
        if (prop.isId()) {
            return this;
        }
        return createFetcher(prop, loader);
    }

    private FetcherImpl<E> realRecursiveChild(FetcherImpl<E> recursivePropHolder) {
        FetcherImpl<E> realRecursiveChild = new FetcherImpl<>(this.getJavaClass());
        ArrayList<FetcherImpl<E>> subFetchers = new ArrayList<>();
        for (FetcherImpl<E> f = this; f != null; f = f.prev) {
            if (!f.negative && f.recursionStrategy == null) {
                subFetchers.add(f);
            }
        }
        for (int i = subFetchers.size() - 1; i >= 0; --i) {
            FetcherImpl<E> subFetcher = subFetchers.get(i);
            realRecursiveChild = new FetcherImpl<>(realRecursiveChild, subFetcher, subFetcher.childFetcher);
        }
        if (recursivePropHolder.prop.isColumnDefinition()) {
            realRecursiveChild = new FetcherImpl<>(realRecursiveChild, recursivePropHolder, null);
        }
        return realRecursiveChild;
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            h = immutableType.hashCode() ^ getFieldMap().hashCode();
            if (h == 0) {
                h = -1;
            }
            this.hash = h;
        }
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Fetcher<?>)) {
            return false;
        }
        Fetcher<?> other = (Fetcher<?>) obj;
        return this.immutableType == other.getImmutableType() &&
                this.getFieldMap().equals(other.getFieldMap());
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
    public boolean __isSimpleFetcher() {
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

    protected FetcherImpl<E> createFetcher(ImmutableProp prop, boolean negative, IdOnlyFetchType idOnlyFetchType) {
        return new FetcherImpl<>(this, prop, negative, idOnlyFetchType);
    }

    protected FetcherImpl<E> createFetcher(ImmutableProp prop, FieldConfig<?, ? extends Table<?>> fieldConfig) {
        return new FetcherImpl<>(this, prop, fieldConfig);
    }

    private static FetcherImpl<?> standardChildFetcher(FieldConfigImpl<?, Table<?>> loaderImpl) {
        FetcherImpl<?> childFetcher = loaderImpl.getChildFetcher();
        if (!(loaderImpl.getProp().isColumnDefinition())) {
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

    private static void validateRecursiveProp(ImmutableProp immutableProp) {
        if (!immutableProp.isAssociation(TargetLevel.ENTITY)) {
            throw new IllegalArgumentException(
                    "Fetcher field based on \"" +
                            immutableProp +
                            "\" cannot be recursive because it is the property is not association"
            );
        }
        if (!immutableProp.getDeclaringType().isEntity()) {
            throw new IllegalArgumentException(
                    "Fetcher field based on \"" +
                            immutableProp +
                            "\" cannot be recursive because the declaring type \"" +
                            immutableProp.getDeclaringType() +
                            "\" is not entity type"
            );
        }
        if (!immutableProp.getDeclaringType().isAssignableFrom(immutableProp.getTargetType())) {
            throw new IllegalArgumentException(
                    "Fetcher field based on \"" +
                            immutableProp +
                            "\" cannot be recursive because the declaring type \"" +
                            immutableProp.getDeclaringType() +
                            "\" is not assignable from the target type \"" +
                            immutableProp.getTargetType() +
                            "\""
            );
        }
    }

    private static void validateConfig(ImmutableProp immutableProp, FieldConfigImpl<Object, Table<Object>> loaderImpl) {
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
}
