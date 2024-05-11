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
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class FetcherImpl<E> implements FetcherImplementor<E> {

    final FetcherImpl<E> prev;

    private final ImmutableType immutableType;

    final boolean negative;

    final boolean implicit;

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
            this.implicit = base.implicit;
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
            this.implicit = false;
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

    protected FetcherImpl(
            FetcherImpl<E> prev,
            ImmutableProp prop,
            boolean negative,
            IdOnlyFetchType idOnlyFetchType
    ) {
        this.prev = prev;
        this.immutableType = prev.immutableType;
        this.negative = negative;
        this.implicit = false;
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
        this.implicit = false;
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
        this.implicit = false;
        this.rawId = base.rawId;
        this.prop = base.prop;
        this.filter = base.filter;
        this.batchSize = base.batchSize;
        this.limit = base.limit;
        this.offset = base.offset;
        this.recursionStrategy = child != null ? base.recursionStrategy : null;
        this.childFetcher = child;
    }

    public FetcherImpl(
            FetcherImpl<E> prev,
            ImmutableProp prop,
            FetcherImpl<?> child,
            boolean implicit
    ) {
        this.prev = prev;
        this.immutableType = prev != null ? prev.immutableType : prop.getDeclaringType();
        this.negative = false;
        this.implicit = implicit;
        this.rawId = false;
        this.prop = prop;
        this.filter = null;
        this.batchSize = prev != null ? prev.batchSize : 0;
        this.limit = Integer.MAX_VALUE;
        this.offset = 0;
        this.recursionStrategy = null;
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
                if (fetcher.prop == null) {
                    continue;
                }
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
                            fetcher.implicit,
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
                for (Dependency dependency : getLeafDependencies(field.getProp())) {
                    ImmutableProp depProp = dependency.getProps().get(0);
                    ImmutableProp deeperDepProp = dependency.getProps().size() > 1 ? dependency.getProps().get(1) : null;
                    Field dependencyField = orderedMap.get(depProp.getName());
                    if (dependencyField == null) {
                        FetcherImpl<?> childFetcher;
                        if (field.getProp().getManyToManyViewBaseProp() != null) {
                            childFetcher = (FetcherImpl<?>)
                                    (new FetcherImpl<>((Class<Object>) depProp.getTargetType().getJavaClass())
                                            .add(
                                                    deeperDepProp.getName(),
                                                    field.getChildFetcher()
                                            )
                                    );
                        } else if (deeperDepProp != null) {
                            childFetcher = createFormulaChildFetcher(dependency);
                        } else {
                            childFetcher = null;
                        }
                        dependencyField = new FieldImpl(
                                immutableType,
                                depProp,
                                deeperDepProp != null && filter != null ?
                                        new MiddleEntityJoinFieldFilter(
                                                (FieldFilter<Table<?>>) field.getFilter(),
                                                deeperDepProp.getName()
                                        ) :
                                        null,
                                field.getBatchSize(),
                                field.getLimit(),
                                field.getOffset(),
                                null,
                                childFetcher,
                                true,
                                false
                        );
                        orderedMap.put(depProp.getName(), dependencyField);
                        if (prop != null && prop.isFormula() && !(prop.getSqlTemplate() instanceof FormulaTemplate)) {
                            extensionFields.add(dependencyField);
                        }
                    } else if (deeperDepProp != null) {
                        FetcherImplementor<?> childFetcher = (FetcherImplementor<?>) dependencyField.getChildFetcher();
                        try {
                            if (prop != null && prop.getManyToManyViewBaseProp() != null) {
                                Field deeperField = childFetcher.getFieldMap().get(deeperDepProp.getName());
                                Fetcher<?> deeperFetcher = deeperField != null ? deeperField.getChildFetcher() : null;
                                childFetcher = childFetcher.add(
                                        deeperDepProp.getName(),
                                        new FetcherMergeContext().merge(
                                                field.getChildFetcher(),
                                                deeperFetcher,
                                                depProp.isEmbedded(EmbeddedLevel.SCALAR)
                                        )
                                );
                            } else {
                                childFetcher =
                                        (FetcherImplementor<?>) new FetcherMergeContext().merge(
                                                createFormulaChildFetcher(dependency),
                                                childFetcher,
                                                depProp.isEmbedded(EmbeddedLevel.SCALAR)
                                        );
                            }
                        } catch (FetcherMergeContext.ConflictException ex) {
                            throw new IllegalArgumentException(
                                    "Cannot merge the fetcher field \"" +
                                            field.getProp().getName() +
                                            ex.path +
                                            "\" and \"" +
                                            depProp.getName() +
                                            '.' +
                                            deeperDepProp.getName() +
                                            ex.path +
                                            "\", the configuration `" +
                                            ex.cfgName +
                                            "` is conflict"
                            );
                        }
                        dependencyField = new FieldImpl(
                                (FieldImpl) dependencyField,
                                (FetcherImpl<?>) childFetcher
                        );
                        orderedMap.put(depProp.getName(), dependencyField);
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
                ImmutableProp prop = field.getProp();
                if (!prop.getDependencies().isEmpty()) {
                    continue;
                }
                if (prop.hasTransientResolver() || prop.isAssociation(TargetLevel.ENTITY)) {
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
    public Fetcher<E> allReferenceFields() {
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
    @Override
    @SuppressWarnings("unchecked")
    public FetcherImplementor<E> add(
            String prop,
            Fetcher<?> childFetcher,
            Consumer<? extends FieldConfig<?, ? extends Table<?>>> loaderBlock
    ) {
        Objects.requireNonNull(prop, "'prop' cannot be null");
        ImmutableProp immutableProp = immutableType.getProp(prop);
        if (childFetcher != null &&
                !immutableProp.isAssociation(TargetLevel.ENTITY) &&
                !immutableProp.isEmbedded(EmbeddedLevel.SCALAR)) {
            throw new IllegalArgumentException(
                    "Cannot load the property \"" +
                            immutableProp +
                            "\" with child fetcher because it is neither association nor embeddable"
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
        ArrayList<FetcherImpl<E>> subFetchers = new ArrayList<>();
        for (FetcherImpl<E> f = this; f != null; f = f.prev) {
            if (f.recursionStrategy == null) {
                subFetchers.add(f);
            }
        }
        FetcherImpl<E> realRecursiveChild = new FetcherImpl<>(this.getJavaClass());
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
            if (loaderImpl.getLimit() != Integer.MAX_VALUE || loaderImpl.getOffset() != 0) {
                if (immutableProp.isRemote()) {
                    throw new IllegalArgumentException(
                            "Fetcher field based one \"" +
                                    immutableProp +
                                    "\" does not support `pagination` because the association is remote"
                    );
                }
                if (!immutableProp.isReferenceList(TargetLevel.PERSISTENT)) {
                    throw new IllegalArgumentException(
                            "Fetcher field based one \"" +
                                    immutableProp +
                                    "\" does not support `pagination` because the association is not list(one-to-many/many-to-many)"
                    );
                }
            }
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

    @SuppressWarnings("unchecked")
    private static FetcherImpl<?> createFormulaChildFetcher(Dependency dependency) {
        List<ImmutableProp> props = dependency.getProps();
        if (props.size() < 2) {
            return null;
        }
        FetcherImpl<?> childFetcher = null;
        for (int i = props.size() - 1; i > 0; --i) {
            ImmutableProp prop = props.get(i);
            childFetcher = new FetcherImpl<>(
                    prop.getDeclaringType().isEntity() ? new FetcherImpl<>(prop.getDeclaringType().getJavaClass()) : null,
                    prop,
                    childFetcher,
                    true
            );
        }
        return childFetcher;
    }

    private static List<Dependency> getLeafDependencies(ImmutableProp prop) {
        return getLeafDependenciesStream(prop).collect(toList());
    }

    private static Stream<Dependency> getLeafDependenciesStream(ImmutableProp prop) {
        return prop.getDependencies().stream().flatMap(d -> {
            ImmutableProp depProp = d.getProps().get(0);
            return depProp.isFormula() ? getLeafDependenciesStream(depProp) : Stream.of(d);
        });
    }
}
