package org.babyfish.jimmer.meta.impl;

import kotlin.jvm.internal.ClassBasedDeclarationContainer;
import kotlin.reflect.KClass;
import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.View;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.meta.LogicalDeletedValueGenerator;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.SqlContext;
import org.babyfish.jimmer.sql.meta.impl.IdGenerators;
import org.babyfish.jimmer.sql.meta.impl.LogicalDeletedValueGenerators;
import org.babyfish.jimmer.sql.meta.impl.MetaCache;
import org.babyfish.jimmer.sql.meta.impl.SqlContextCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

class ImmutableTypeImpl extends AbstractImmutableTypeImpl {

    @SuppressWarnings("unchecked")
    private static final Class<? extends Annotation>[] SQL_ANNOTATION_TYPES = new Class[] {
            Entity.class,
            MappedSuperclass.class,
            Embeddable.class
    };

    private static final IdGenerator NIL_ID_GENERATOR = new IdGenerator() {};

    private static final LogicalDeletedValueGenerator<?> NIL_LOGICAL_DELETED_VALUE_GENERATOR = new LogicalDeletedValueGenerator<Object>() {
        @Override
        public Object generate() {
            throw new UnsupportedOperationException();
        }
    };

    private final Class<?> javaClass;

    private final boolean isEntity;

    private final boolean isMappedSupperClass;

    private final boolean isEmbeddable;

    private final Annotation immutableAnnotation;

    private KClass<?> kotlinClass;

    private final ImmutableType primarySuperType;

    private final Set<ImmutableType> superTypes;

    private Set<ImmutableType> allTypes;

    private final BiFunction<DraftContext, Object, Draft> draftFactory;

    private Map<String, ImmutableProp> declaredProps;

    private Map<String, ImmutableProp> props;

    private ImmutableProp[] propArr;

    private Map<String, List<ImmutableProp>> embeddedPaths;

    private Map<String, ImmutableProp> entityProps;

    private Map<String, ImmutableProp> selectableProps;

    private Map<String, ImmutableProp> selectableScalarProps;

    private Map<String, ImmutableProp> selectableReferenceProps;

    private Map<String, ImmutableProp> objectCacheProps;

    private Map<String, ImmutableProp> referenceProps;

    private ImmutableProp idProp;

    private ImmutableProp versionProp;

    private LogicalDeletedInfo declaredLogicalDeletedInfo;

    private LogicalDeletedInfo logicalDeletedInfo;

    private KeyMatcher keyMatcher = KeyMatcher.EMPTY;

    private final String microServiceName;

    private final MetaCache<String> tableNameCache = new MetaCache<>(this::getTableName0);

    private final SqlContextCache<IdGenerator> idGeneratorCache = new SqlContextCache<>(it -> {
        IdGenerator g = IdGenerators.of(this, it);
        return g != null ? g : NIL_ID_GENERATOR;
    });

    private final SqlContextCache<LogicalDeletedValueGenerator<?>> logicalDeletedValueGeneratorCache = new SqlContextCache<>(it -> {
        LogicalDeletedValueGenerator<?> g = LogicalDeletedValueGenerators.of(getLogicalDeletedInfo(), it);
        return g != null ? g : NIL_LOGICAL_DELETED_VALUE_GENERATOR;
    });

    ImmutableTypeImpl(
            Class<?> javaClass,
            Set<ImmutableType> superTypes,
            BiFunction<DraftContext, Object, Draft> draftFactory
    ) {
        Annotation sqlAnnotation = null;
        for (Class<? extends Annotation> sqlAnnotationType : SQL_ANNOTATION_TYPES) {
            Annotation anno = javaClass.getAnnotation(sqlAnnotationType);
            if (anno != null) {
                if (sqlAnnotation != null) {
                    throw new ModelException(
                            "Illegal type \"" +
                                    javaClass.getName() +
                                    "\", it cannot be decorated by both @" +
                                    sqlAnnotation.annotationType().getName() +
                                    " and @" +
                                    anno.annotationType().getName()
                    );
                }
                sqlAnnotation = anno;
            }
        }
        if (sqlAnnotation != null) {
            immutableAnnotation = sqlAnnotation;
        } else {
            immutableAnnotation = javaClass.getAnnotation(Immutable.class);
            if (immutableAnnotation == null) {
                throw new ModelException(
                        "Illegal type \"" +
                                javaClass.getName() +
                                "\", it is not immutable type"
                );
            }
        }

        this.javaClass = javaClass;
        this.primarySuperType = superTypes.stream().filter(it -> !it.isMappedSuperclass()).findFirst().orElse(null);
        this.superTypes = Collections.unmodifiableSet(superTypes);

        this.draftFactory = draftFactory;

        isEntity = immutableAnnotation instanceof Entity;
        isMappedSupperClass = immutableAnnotation instanceof MappedSuperclass;
        isEmbeddable = immutableAnnotation instanceof Embeddable;

        if (isEntity) {
            microServiceName = javaClass.getAnnotation(Entity.class).microServiceName();
        } else if (isMappedSupperClass) {
            microServiceName = javaClass.getAnnotation(MappedSuperclass.class).microServiceName();
        } else {
            microServiceName = "";
        }
    }

    ImmutableTypeImpl(
            KClass<?> kotlinClass,
            Set<ImmutableType> superTypes,
            BiFunction<DraftContext, Object, Draft> draftFactory
    ) {
        this(((ClassBasedDeclarationContainer)kotlinClass).getJClass(), superTypes, draftFactory);
        this.kotlinClass = kotlinClass;
    }

    @NotNull
    @Override
    public Class<?> getJavaClass() {
        return javaClass;
    }

    @Override
    public boolean isKotlinClass() {
        return kotlinClass != null;
    }

    @Override
    public boolean isEntity() {
        return isEntity;
    }

    @Override
    public boolean isMappedSuperclass() {
        return isMappedSupperClass;
    }

    @Override
    public boolean isEmbeddable() {
        return isEmbeddable;
    }

    @NotNull
    @Override
    public Annotation getImmutableAnnotation() {
        return immutableAnnotation;
    }

    @Nullable
    KClass<?> getKotlinClass() { return kotlinClass; }

    @Override
    public boolean isAssignableFrom(ImmutableType type) {
        return javaClass.isAssignableFrom(type.getJavaClass());
    }

    @Nullable
    @Override
    public ImmutableType getPrimarySuperType() {
        return primarySuperType;
    }

    @Override
    public Set<ImmutableType> getSuperTypes() {
        return superTypes;
    }

    @Override
    public Set<ImmutableType> getAllTypes() {
        Set<ImmutableType> all = allTypes;
        if (all == null) {
            all = new LinkedHashSet<>();
            collectAllSuperTypes(all);
            allTypes = all = Collections.unmodifiableSet(all);
        }
        return all;
    }

    private void collectAllSuperTypes(Set<ImmutableType> allSuperTypes) {
        allSuperTypes.add(this);
        for (ImmutableType superType : superTypes) {
            ((ImmutableTypeImpl)superType).collectAllSuperTypes(allSuperTypes);
        }
    }

    @NotNull
    @Override
    public BiFunction<DraftContext, Object, Draft> getDraftFactory() {
        return draftFactory;
    }

    @NotNull
    @Override
    public Map<String, ImmutableProp> getDeclaredProps() {
        return declaredProps;
    }

    @Override
    public ImmutableProp getIdProp() {
        return idProp;
    }

    @Nullable
    @Override
    public ImmutableProp getVersionProp() {
        return versionProp;
    }

    @Nullable
    @Override
    public LogicalDeletedInfo getDeclaredLogicalDeletedInfo() {
        return declaredLogicalDeletedInfo;
    }

    @Nullable
    @Override
    public LogicalDeletedInfo getLogicalDeletedInfo() {
        return logicalDeletedInfo;
    }

    @NotNull
    @Override
    public KeyMatcher getKeyMatcher() {
        return keyMatcher;
    }

    @NotNull
    @Override
    public Map<String, ImmutableProp> getProps() {
        return props;
    }

    private Map<String, ImmutableProp> createPropMap(Map<String, PropId> redirectedMap) {
        Map<String, ImmutableProp> propMap = new LinkedHashMap<>();
        for (ImmutableType superType : superTypes) {
            for (ImmutableProp prop : superType.getProps().values()) {
                if (!propMap.containsKey(prop.getName())) {
                    propMap.putIfAbsent(
                            prop.getName(),
                            new ImmutablePropImpl(
                                    (ImmutablePropImpl) prop,
                                    this,
                                    redirectedMap.get(prop.getName())
                            )
                    );
                }
            }
        }
        for (ImmutableProp declaredProp : declaredProps.values()) {
            ImmutableProp conflictProp = propMap.put(declaredProp.getName(), declaredProp);
            if (conflictProp != null && conflictProp != ((ImmutablePropImpl)declaredProp).getOriginal()) {
                throw new ModelException(
                        "The property \"" +
                                declaredProp +
                                "\" overrides property of super type, this is not allowed"
                );
            }
        }
        return propMap;
    }

    @NotNull
    @Override
    public ImmutableProp getProp(String name) {
        ImmutableProp prop = getProps().get(name);
        if (prop == null) {
            throw new IllegalArgumentException(
                    "There is no property \"" + name + "\" in \"" + this + "\""
            );
        }
        return prop;
    }

    @NotNull
    @Override
    public ImmutableProp getProp(PropId id) {
        int index = id.asIndex();
        if (index == -1) {
            return getProp(id.asName());
        }
        ImmutableProp[] arr = this.getPropArr();
        if (index < 0 || index >= arr.length) {
            throw new IllegalArgumentException(
                    "There is no property whose id is " + id + " in \"" + this + "\""
            );
        }
        return arr[index];
    }

    @NotNull
    private ImmutableProp[] getPropArr() {
        ImmutableProp[] arr = propArr;
        if (arr == null) {
            if (isMappedSupperClass) {
                arr = getProps().values().toArray(new ImmutableProp[0]);
            } else {
                arr = new ImmutableProp[getProps().size() + 1];
                for (ImmutableProp prop : getProps().values()) {
                    arr[prop.getId().asIndex()] = prop;
                }
            }
            propArr = arr;
        }
        return arr;
    }

    @Override
    public Map<String, List<ImmutableProp>> getEmbeddedPaths() {
        Map<String, List<ImmutableProp>> paths = embeddedPaths;
        if (paths == null) {
            paths = new LinkedHashMap<>();
            collectEmbeddedPaths(new ArrayList<>(), paths);
            embeddedPaths = paths;
        }
        return paths;
    }

    private void collectEmbeddedPaths(ArrayList<ImmutableProp> stack, Map<String, List<ImmutableProp>> pathMap) {
        if (!isEmbeddable) {
            return;
        }
        for (ImmutableProp prop : getProps().values()) {
            if (prop.isFormula()) {
                continue;
            }
            stack.add(prop);
            try {
                ImmutableType targetType = prop.getTargetType();
                if (targetType != null) {
                    ((ImmutableTypeImpl)targetType).collectEmbeddedPaths(stack, pathMap);
                } else {
                    pathMap.put(
                            stack.stream().map(ImmutableProp::getName).collect(Collectors.joining(".")),
                            Collections.unmodifiableList(new ArrayList<>(stack))
                    );
                }
            } finally {
                stack.remove(stack.size() - 1);
            }
        }
    }

    @NotNull
    @Override
    public Map<String, ImmutableProp> getEntityProps() {
        Map<String, ImmutableProp> map = entityProps;
        if (map == null) {
            if (isEntity) {
                map = primarySuperType != null && primarySuperType.isEntity() ?
                        declaredProps :
                        props;
            } else {
                map = Collections.emptyMap();
            }
            entityProps = map;
        }
        return map;
    }

    public Map<String, ImmutableProp> getSelectableProps() {
        Map<String, ImmutableProp> map = selectableProps;
        if (map == null) {
            map = new LinkedHashMap<>();
            map.put(idProp.getName(), idProp);
            for (ImmutableProp prop : getProps().values()) {
                if (!prop.isId() && prop.isColumnDefinition()) {
                    map.put(prop.getName(), prop);
                }
            }
            selectableProps = map = Collections.unmodifiableMap(map);
        }
        return map;
    }

    @Override
    public Map<String, ImmutableProp> getSelectableScalarProps() {
        Map<String, ImmutableProp> map = selectableScalarProps;
        if (map == null) {
            map = new LinkedHashMap<>();
            for (ImmutableProp prop : getSelectableProps().values()) {
                if (!prop.isAssociation(TargetLevel.ENTITY) &&
                        !prop.isLogicalDeleted() &&
                        !prop.isExcludedFromAllScalars()
                ) {
                    map.put(prop.getName(), prop);
                }
            }
            selectableScalarProps = map = Collections.unmodifiableMap(map);
        }
        return map;
    }

    public Map<String, ImmutableProp> getSelectableReferenceProps() {
        Map<String, ImmutableProp> map = selectableReferenceProps;
        if (map == null) {
            map = new LinkedHashMap<>();
            for (ImmutableProp prop : getProps().values()) {
                if (prop.isReference(TargetLevel.PERSISTENT) && prop.isColumnDefinition()) {
                    map.put(prop.getName(), prop);
                }
            }
            selectableReferenceProps = map = Collections.unmodifiableMap(map);
        }
        return map;
    }

    @Override
    public Map<String, ImmutableProp> getReferenceProps() {
        Map<String, ImmutableProp> map = referenceProps;
        if (map == null) {
            map = new LinkedHashMap<>();
            for (ImmutableProp prop : getProps().values()) {
                if (prop.isReference(TargetLevel.PERSISTENT)) {
                    map.put(prop.getName(), prop);
                }
            }
            referenceProps = map = Collections.unmodifiableMap(map);
        }
        return map;
    }

    @Override
    public Map<String, ImmutableProp> getObjectCacheProps() {
        Map<String, ImmutableProp> map = objectCacheProps;
        if (map == null) {
            for (ImmutableProp prop : getProps().values()) {
                if (prop.isFormula() && prop.getSqlTemplate() != null) {
                    if (map == null) {
                        map = new LinkedHashMap<>(getSelectableProps());
                    }
                    map.put(prop.getName(), prop);
                }
            }
            if (map == null) {
                map = getSelectableProps();
            } else {
                map = Collections.unmodifiableMap(map);
            }
            objectCacheProps = map;
        }
        return map;
    }

    void setProps(Map<String, ImmutableProp> declaredPropMap, Map<String, PropId> redefinedMap) {
        this.declaredProps = Collections.unmodifiableMap(declaredPropMap);
        this.props = Collections.unmodifiableMap(createPropMap(redefinedMap));
    }

    void setIdProp(ImmutableProp idProp) {
        if (idProp.getDeclaringType() != this) {
            idProp = getProp(idProp.getName());
        }
        if (idProp.isEmbedded(EmbeddedLevel.SCALAR)) {
            validateEmbeddedIdType(idProp.getTargetType(), null);
        }
        this.idProp = idProp;
    }

    void setVersionProp(ImmutableProp versionProp) {
        if (versionProp != null && versionProp.getDeclaringType() != this) {
            versionProp = getProp(versionProp.getName());
        }
        this.versionProp = versionProp;
    }

    void setDeclaredLogicalDeletedInfo(LogicalDeletedInfo declaredLogicalDeletedInfo) {
        LogicalDeletedInfo superInfo = null;
        for (ImmutableType superType : superTypes) {
            superInfo = superType.getLogicalDeletedInfo();
            if (superInfo != null) {
                break;
            }
        }
        if (superInfo != null && declaredLogicalDeletedInfo != null) {
            throw new AssertionError(
                    "Internal bug, @LogicalDeleted field is configured in both \"" +
                            this +
                            "\" and its super type"
            );
        }
        this.declaredLogicalDeletedInfo = declaredLogicalDeletedInfo;
        if (superInfo != null) {
            logicalDeletedInfo = superInfo.to(
                    getProp(superInfo.getProp().getName())
            );
        } else {
            logicalDeletedInfo = declaredLogicalDeletedInfo;
        }
    }

    void setKeyGroups(Map<String, Set<ImmutableProp>> keyGroups) {
        if (keyGroups.isEmpty()) {
            this.keyMatcher = KeyMatcher.EMPTY;
        } else {
            this.keyMatcher = KeyMatcher.of(this, keyGroups);
        }
    }

    @Override
    public String getMicroServiceName() {
        return microServiceName;
    }

    @Override
    public String getTableName(MetadataStrategy strategy) {
        return tableNameCache.get(strategy);
    }

    private String getTableName0(MetadataStrategy strategy) {
        Table table = javaClass.getAnnotation(Table.class);
        String tableName = table != null ? table.name() : "";
        return tableName.isEmpty() ?
                strategy.getNamingStrategy().tableName(this) :
                Utils.resolveMetaString(tableName, strategy.getMetaStringResolver());
    }

    @Override
    public IdGenerator getIdGenerator(SqlContext sqlContext) {
        IdGenerator generator = idGeneratorCache.get(sqlContext);
        return generator == NIL_ID_GENERATOR ? null : generator;
    }

    @Override
    public LogicalDeletedValueGenerator<?> getLogicalDeletedValueGenerator(SqlContext sqlContext) {
        LogicalDeletedValueGenerator<?> generator = logicalDeletedValueGeneratorCache.get(sqlContext);
        return generator == NIL_LOGICAL_DELETED_VALUE_GENERATOR ? null : generator;
    }

    @Override
    public String toString() {
        return javaClass.getName();
    }

    private void validateEntity() {
        if (!isEntity) {
            throw new IllegalStateException(
                    "The current type \"" +
                            this +
                            "\" is not entity"
            );
        }
    }

    private void validateEmbeddedIdType(ImmutableType type, String path) {
        String prefix = path != null ? path + '.' : "";
        for (ImmutableProp prop : type.getProps().values()) {
            if (prop.isNullable()) {
                throw new ModelException(
                        "Illegal id property \"" +
                                this +
                                "\", the embedded property \"" +
                                prefix + prop.getName() +
                                "\" cannot be nullable"
                );
            }
            if (prop.isEmbedded(EmbeddedLevel.SCALAR)) {
                validateEmbeddedIdType(prop.getTargetType(), prefix + prop.getName());
            }
        }
    }

    public static class BuilderImpl implements Builder {

        private final KClass<?> kotlinType;

        private final Class<?> javaClass;

        private final Set<ImmutableType> superTypes;

        private final BiFunction<DraftContext, Object, Draft> draftFactory;

        private String idPropName;

        private String versionPropName;

        private String logicalDeletedPropName;

        private final List<String> keyPropNames = new ArrayList<>();

        private final Map<String, PropBuilder> propBuilderMap = new LinkedHashMap<>();

        private final Set<PropId> propIds = new LinkedHashSet<>();

        private final Map<String, PropId> redefinedMap = new HashMap<>();

        BuilderImpl(
                Class<?> javaClass,
                Collection<ImmutableType> superTypes,
                BiFunction<DraftContext, Object, Draft> draftFactory
        ) {
            if (View.class.isAssignableFrom(javaClass)) {
                throw new ModelException(
                        "Illegal type \"" +
                                javaClass.getName() +
                                "\", immutable type can not inherit \"" +
                                View.class.getName() +
                                "\""
                );
            }
            this.kotlinType = null;
            this.javaClass = javaClass;
            this.superTypes = standardSuperTypes(superTypes);
            this.draftFactory = draftFactory;
            for (ImmutableType superType : superTypes) {
                for (ImmutableProp prop : superType.getProps().values()) {
                    PropId id = prop.getId();
                    if (id.asIndex() != -1) {
                        propIds.add(id);
                    }
                }
            }
        }

        BuilderImpl(
                KClass<?> kotlinType,
                Collection<ImmutableType> superTypes,
                BiFunction<DraftContext, Object, Draft> draftFactory
        ) {
            Class<?> javaClass = ((ClassBasedDeclarationContainer)kotlinType).getJClass();
            if (View.class.isAssignableFrom(javaClass)) {
                throw new ModelException(
                        "Illegal type \"" +
                                javaClass.getName() +
                                "\", immutable type can not inherit \"" +
                                View.class.getName() +
                                "\""
                );
            }
            this.kotlinType = kotlinType;
            this.javaClass = javaClass;
            this.superTypes = standardSuperTypes(superTypes);
            this.draftFactory = draftFactory;
            for (ImmutableType superType : superTypes) {
                for (ImmutableProp prop : superType.getProps().values()) {
                    PropId id = prop.getId();
                    if (id.asIndex() != -1) {
                        propIds.add(id);
                    }
                }
            }
        }

        private Set<ImmutableType> standardSuperTypes(Collection<ImmutableType> superTypes) {
            if (superTypes.isEmpty()) {
                return Collections.emptySet();
            }
            if (javaClass.isAnnotationPresent(Embeddable.class)) {
                throw new ModelException(
                        "Illegal type \"" +
                                javaClass.getName() +
                                "\", embeddable type does not support inheritance"
                );
            }
            Set<ImmutableType> set = new LinkedHashSet<>(superTypes);
            if (javaClass.isAnnotationPresent(Immutable.class)) {
                if (set.size() > 1) {
                    throw new ModelException(
                            "Illegal type \"" +
                                    javaClass.getName() +
                                    "\", simple immutable type does not support multiple inheritance"
                    );
                }
                if (!set.iterator().next().getJavaClass().isAnnotationPresent(Immutable.class)) {
                    throw new ModelException(
                            "Illegal type \"" +
                                    javaClass.getName() +
                                    "\", simple immutable type can only inherit simple immutable type"
                    );
                }
            } else {
                for (ImmutableType superType : set) {
                    if (superType.isEntity()) {
                        if (javaClass.isAnnotationPresent(Entity.class)) {
                            throw new ModelException(
                                    "Illegal type \"" +
                                            javaClass.getName() +
                                            "\", inheriting from entity type is not supported now, " +
                                            "it will be supported in the future"
                            );
                        } else {
                            throw new ModelException(
                                    "Illegal type \"" +
                                            javaClass.getName() +
                                            "\", mapped super class cannot inherit entity type"
                            );
                        }
                    } else if (!superType.isMappedSuperclass()){
                        throw new ModelException(
                                "Illegal type \"" +
                                        javaClass.getName() +
                                        "\", the super type \"" +
                                        superType +
                                        "\"" +
                                        (
                                                javaClass.isAnnotationPresent(Entity.class) ?
                                                        " is neither entity nor mapped super class" :
                                                        " is not mapped super class"
                                        )
                        );
                    }
                }
            }
            if (set.size() > 1) {
                Map<String, ImmutableProp> superPropMap = new HashMap<>();
                for (ImmutableType superType : superTypes) {
                    for (ImmutableProp prop : superType.getProps().values()) {
                        ImmutableProp oldProp = superPropMap.put(prop.getName(), prop);
                        if (oldProp != null) {
                            if (oldProp.getCategory() != prop.getCategory()) {
                                throw new ModelException(
                                        "Illegal type \"" +
                                                javaClass.getName() +
                                                "\", conflict super properties: \"" +
                                                oldProp +
                                                "\" and \"" +
                                                prop +
                                                "\", this first one is " +
                                                oldProp.getCategory() +
                                                " and the second is " +
                                                prop.getCategory()
                                );
                            }
                            if (!oldProp.getGenericType().equals(prop.getGenericType())) {
                                throw new ModelException(
                                        "Illegal type \"" +
                                                javaClass.getName() +
                                                "\", conflict super properties: \"" +
                                                oldProp +
                                                "\" and \"" +
                                                prop +
                                                "\", their types are different"
                                );
                            }
                            if (oldProp.isNullable() != prop.isNullable()) {
                                throw new ModelException(
                                        "Illegal type \"" +
                                                javaClass.getName() +
                                                "\", conflict super properties: \"" +
                                                oldProp +
                                                "\" and \"" +
                                                prop +
                                                "\", their nullity are different"
                                );
                            }
                            if (oldProp.isInputNotNull() != prop.isInputNotNull()) {
                                throw new ModelException(
                                        "Illegal type \"" +
                                                javaClass.getName() +
                                                "\", conflict super properties: \"" +
                                                oldProp +
                                                "\" and \"" +
                                                prop +
                                                "\", their input nullity are different"
                                );
                            }
                            if (!((ImmutablePropImpl)oldProp).getMappedByValue().equals(((ImmutablePropImpl)prop).getMappedByValue())) {
                                throw new ModelException(
                                        "Illegal type \"" +
                                                javaClass.getName() +
                                                "\", conflict super properties: \"" +
                                                oldProp +
                                                "\" and \"" +
                                                prop +
                                                "\", their configuration `mappedBy` are different"
                                );
                            }
                            if (oldProp.getPrimaryAnnotationType() != prop.getPrimaryAnnotationType()) {
                                throw new ModelException(
                                        "Illegal type \"" +
                                                javaClass.getName() +
                                                "\", conflict super properties: \"" +
                                                oldProp +
                                                "\" and \"" +
                                                prop +
                                                "\", the first one is decorated by \"@" +
                                                oldProp.getPrimaryAnnotationType().getName() +
                                                "\" but the second one is decorated by \"@" +
                                                prop.getPrimaryAnnotationType().getName() +
                                                "\""
                                );
                            }
                        }
                    }
                }
            }
            return Collections.unmodifiableSet(set);
        }

        @Override
        public Builder redefine(String name, int id) {
            ImmutableProp superProp = null;
            for (ImmutableType superType : superTypes) {
                superProp = superType.getProps().get(name);
                if (superProp != null) {
                    break;
                }
            }
            if (superProp == null) {
                throw new IllegalArgumentException(
                        "Cannot redefine \"" +
                                name +
                                "\" because there is no such property in super types"
                );
            }
            if (superProp.getId().asIndex() != -1) {
                throw new IllegalArgumentException(
                        "Cannot redefine the property \"" +
                                name +
                                "\" because it is already based on integer id"
                );
            }
            redefinedMap.put(name, PropId.byIndex(id));
            return this;
        }

        @Override
        public Builder id(int id, String name, Class<?> elementType) {
            if (!javaClass.isAnnotationPresent(Entity.class)
                    && !javaClass.isAnnotationPresent(MappedSuperclass.class)) {
                throw new IllegalStateException(
                        "Cannot set id property for type \"" +
                                javaClass.getName() +
                                "\" which is not entity or mapped super class"
                );
            }
            for (ImmutableType superType : superTypes) {
                if (superType.getIdProp() != null) {
                    throw new IllegalStateException(
                            "Cannot set id property for type \"" +
                                    javaClass.getName() +
                                    "\" because there is an id property in the super type \"" +
                                    superType.getJavaClass().getName() +
                                    "\""
                    );
                }
            }
            if (idPropName != null) {
                throw new IllegalStateException(
                        "Conflict id properties \"" +
                                idPropName +
                                "\" and \"" +
                                name +
                                "\" in \"" +
                                javaClass.getName() +
                                "\""
                );
            }
            idPropName = name;
            return add(
                    id,
                    name,
                    category(elementType),
                    elementType,
                    false
            );
        }

        @Override
        public Builder key(int id, String name, Class<?> elementType, boolean nullable) {
            if (!javaClass.isAnnotationPresent(Entity.class) &&
                    !javaClass.isAnnotationPresent(MappedSuperclass.class)) {
                throw new IllegalStateException(
                        "Cannot set key property for type \"" +
                                javaClass.getName() +
                                "\" which is not entity or mapped super class"
                );
            }
            keyPropNames.add(name);
            return add(id, name, category(elementType), elementType, nullable);
        }

        @Override
        public Builder keyReference(
                int id,
                String name,
                Class<? extends Annotation> associationAnnotationType,
                Class<?> elementType,
                boolean nullable
        ) {
            if (associationAnnotationType != OneToOne.class && associationAnnotationType != ManyToOne.class) {
                throw new IllegalArgumentException(
                        "The `associationAnnotationType` must be `OneOne` or `ManyToOne`"
                );
            }
            keyPropNames.add(name);
            return add(
                    id,
                    name,
                    ImmutablePropCategory.REFERENCE,
                    elementType,
                    nullable,
                    associationAnnotationType
            );
        }

        @Override
        public Builder version(int id, String name) {
            if (!javaClass.isAnnotationPresent(Entity.class) &&
                    !javaClass.isAnnotationPresent(MappedSuperclass.class)) {
                throw new IllegalStateException(
                        "Cannot set version property for type \"" +
                                javaClass.getName() +
                                "\" which is not entity or mapped super class"
                );
            }
            for (ImmutableType superType : superTypes) {
                if (superType.getVersionProp() != null) {
                    throw new IllegalStateException(
                            "Cannot set version property for type \"" +
                                    javaClass.getName() +
                                    "\" because there is an version property in the super type \"" +
                                    superType.getJavaClass().getName() +
                                    "\""
                    );
                }
            }
            if (versionPropName != null) {
                throw new IllegalStateException(
                        "Conflict version properties \"" +
                                versionPropName +
                                "\" and \"" +
                                name +
                                "\" in \"" +
                                javaClass.getName() +
                                "\""
                );
            }
            versionPropName = name;
            return add(id, name, ImmutablePropCategory.SCALAR, int.class, false);
        }

        @Override
        public Builder logicalDeleted(
                int id,
                String name,
                Class<?> elementType,
                boolean nullable
        ) {
            if (!javaClass.isAnnotationPresent(Entity.class) &&
                    !javaClass.isAnnotationPresent(MappedSuperclass.class)) {
                throw new IllegalStateException(
                        "Cannot set logical deleted property for type \"" +
                                javaClass.getName() +
                                "\" which is not entity or mapped super class"
                );
            }
            for (ImmutableType superType : superTypes) {
                if (superType.getLogicalDeletedInfo() != null) {
                    throw new IllegalStateException(
                            "Cannot set logical deleted property for type \"" +
                                    javaClass.getName() +
                                    "\" because there is an id property in the super type \"" +
                                    superType.getJavaClass().getName() +
                                    "\""
                    );
                }
            }
            if (logicalDeletedPropName != null) {
                throw new IllegalStateException(
                        "Conflict logical deleted properties \"" +
                                logicalDeletedPropName +
                                "\" and \"" +
                                name +
                                "\" in \"" +
                                javaClass.getName() +
                                "\""
                );
            }
            logicalDeletedPropName = name;
            return add(id, name, ImmutablePropCategory.SCALAR, elementType, nullable);
        }

        @Override
        public Builder add(
                int id,
                String name,
                ImmutablePropCategory category,
                Class<?> elementType,
                boolean nullable
        ) {
            return add(
                    id,
                    name,
                    category,
                    elementType,
                    nullable,
                    null
            );
        }

        @Override
        public Builder add(
                int id,
                String name,
                Class<? extends Annotation> associationType,
                Class<?> elementType,
                boolean nullable
        ) {
            ImmutablePropCategory category;
            if (associationType == OneToOne.class || associationType == ManyToOne.class) {
                category = ImmutablePropCategory.REFERENCE;
            } else if (associationType == OneToMany.class || associationType == ManyToMany.class || associationType == ManyToManyView.class) {
                category = ImmutablePropCategory.REFERENCE_LIST;
            } else {
                throw new IllegalArgumentException("Invalid association type");
            }
            return add(
                    id,
                    name,
                    category,
                    elementType,
                    nullable,
                    associationType
            );
        }

        private Builder add(
                int id,
                String name,
                ImmutablePropCategory category,
                Class<?> elementType,
                boolean nullable,
                Class<? extends Annotation> associationType
        ) {
            if (category.isAssociation() &&
                    elementType.isAnnotationPresent(Entity.class) &&
                    !javaClass.isAnnotationPresent(Entity.class) &&
                    !javaClass.isAnnotationPresent(MappedSuperclass.class)) {
                throw new IllegalStateException("Cannot set association for type that is not entity or mapped super class");
            }
            PropId propId;
            if (id == -1) {
                propId = PropId.byName(name);
            } else {
                if (javaClass.isAnnotationPresent(MappedSuperclass.class)) {
                    throw new IllegalArgumentException("The prop-id of properties in mapped super class must be -1");
                }
                propId = PropId.byIndex(id);
                if (!propIds.add(propId)) {
                    throw new IllegalArgumentException(
                            "The property id \"" +
                                    id +
                                    "." +
                                    name +
                                    "\" is already exists in current type or the super type"
                    );
                }
            }
            if (propBuilderMap.containsKey(name)) {
                throw new IllegalArgumentException(
                        "The property \"" +
                                javaClass.getName() +
                                "." +
                                name +
                                "\", it is already exists"
                );
            }
            for (ImmutableType superType : superTypes) {
                ImmutableProp superProp = superType.getProps().get(name);
                if (superProp != null && superProp.getId().asIndex() != -1) {
                    throw new IllegalArgumentException(
                            "The property \"" +
                                    javaClass.getName() +
                                    "." +
                                    name +
                                    "\" is already exists in super type \"" +
                                    superType +
                                    "\""
                    );
                }
            }
            propBuilderMap.put(
                    name,
                    new PropBuilder(
                            propId,
                            name,
                            category,
                            elementType,
                            nullable,
                            associationType
                    )
            );
            return this;
        }

        private static ImmutablePropCategory category(Class<?> elementType) {
            return elementType.isAnnotationPresent(Embeddable.class) ?
                    ImmutablePropCategory.REFERENCE :
                    ImmutablePropCategory.SCALAR;
        }

        @Override
        public ImmutableType build() {

            ImmutableTypeImpl type = kotlinType != null ?
                    new ImmutableTypeImpl(kotlinType, superTypes, draftFactory) :
                    new ImmutableTypeImpl(javaClass, superTypes, draftFactory);

            Map<String, ImmutableProp> map = new LinkedHashMap<>();
            for (Map.Entry<String, PropBuilder> e : propBuilderMap.entrySet()) {
                map.put(e.getKey(), e.getValue().build(type));
            }
            type.setProps(map, redefinedMap);

            if (idPropName != null) {
                type.setIdProp(type.declaredProps.get(idPropName));
            } else {
                for (ImmutableType superType : type.superTypes) {
                    ImmutableProp superIdProp = superType.getIdProp();
                    if (superIdProp != null) {
                        type.setIdProp(superIdProp);
                        break;
                    }
                }
            }

            if (versionPropName != null) {
                type.setVersionProp(type.declaredProps.get(versionPropName));
            } else {
                for (ImmutableType superType : type.superTypes) {
                    ImmutableProp superVersionProp = superType.getVersionProp();
                    if (superVersionProp != null) {
                        type.setVersionProp(superVersionProp);
                        break;
                    }
                }
            }

            if (logicalDeletedPropName != null) {
                type.setDeclaredLogicalDeletedInfo(LogicalDeletedInfo.of(type.declaredProps.get(logicalDeletedPropName)));
            } else {
                type.setDeclaredLogicalDeletedInfo(null);
            }

            Map<String, Set<ImmutableProp>> keyGroupMap = new LinkedHashMap<>();
            for (ImmutableType superType : superTypes) {
                for (Map.Entry<String, Set<ImmutableProp>> e : superType.getKeyMatcher().toMap().entrySet()) {
                    keyGroupMap
                            .computeIfAbsent(e.getKey(), it -> new LinkedHashSet<>())
                            .addAll(e.getValue());
                }
            }
            for (String keyPropName : keyPropNames) {
                ImmutableProp prop = type.declaredProps.get(keyPropName);
                String group = prop.getAnnotation(Key.class).group();
                keyGroupMap
                        .computeIfAbsent(group, it -> new LinkedHashSet<>())
                        .add(prop);
            }
            type.setKeyGroups(keyGroupMap);
            return type;
        }
    }

    private static class PropBuilder {

        final PropId id;
        final String name;
        final ImmutablePropCategory category;
        final Class<?> elementType;
        final boolean nullable;
        final Class<? extends Annotation> associationType;

        PropBuilder(
                PropId id,
                String name,
                ImmutablePropCategory category,
                Class<?> elementType,
                boolean nullable,
                Class<? extends Annotation> associationType
        ) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.elementType = elementType;
            this.nullable = nullable;
            this.associationType = associationType;
        }

        public ImmutableProp build(ImmutableTypeImpl declaringType) {
            return new ImmutablePropImpl(
                    declaringType,
                    id,
                    name,
                    category,
                    elementType,
                    nullable,
                    associationType
            );
        }
    }
}
