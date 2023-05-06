package org.babyfish.jimmer.meta.impl;

import kotlin.jvm.internal.ClassBasedDeclarationContainer;
import kotlin.reflect.KClass;
import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.impl.DatabaseIdentifiers;
import org.babyfish.jimmer.sql.meta.impl.IdGenerators;
import org.babyfish.jimmer.sql.meta.impl.PropChains;
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

    private final Class<?> javaClass;

    private final boolean isEntity;

    private final boolean isMappedSupperClass;

    private final boolean isEmbeddable;

    private final Annotation immutableAnnotation;

    private KClass<?> kotlinClass;

    private final ImmutableType superType;

    private final BiFunction<DraftContext, Object, Draft> draftFactory;

    private Map<String, ImmutableProp> declaredProps;

    private Map<String, ImmutableProp> props;

    private ImmutableProp[] propArr;

    private Map<String, ImmutableProp> selectableProps;

    private Map<String, ImmutableProp> selectableReferenceProps;

    private ImmutableProp idProp;

    private ImmutableProp versionProp;

    private LogicalDeletedInfo declaredLogicalDeletedInfo;

    private LogicalDeletedInfo logicalDeletedInfo;

    private Set<ImmutableProp> keyProps = Collections.emptySet();

    private final String microServiceName;

    private final Map<MetadataStrategy, String> tableNameMap = new HashMap<>();

    private final Map<MetadataStrategy, IdGenerator> idGeneratorMap = new HashMap<>();

    ImmutableTypeImpl(
            Class<?> javaClass,
            ImmutableType superType,
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
        this.superType = superType;
        this.draftFactory = draftFactory;

        isEntity = immutableAnnotation instanceof Entity;
        isMappedSupperClass = immutableAnnotation instanceof MappedSuperclass;
        isEmbeddable = immutableAnnotation instanceof Embeddable;

        if (superType != null) {
            if ((isEntity || isMappedSupperClass) && !superType.isMappedSuperclass()) {
                throw new ModelException(
                        "Illegal immutable type \"" +
                                this +
                                "\", the super type \"" +
                                superType +
                                "\" is not decorated by @" +
                                MappedSuperclass.class.getName()
                );
            }
            if ((superType.isEntity() || superType.isMappedSuperclass()) && !isEntity && !isMappedSupperClass) {
                throw new ModelException(
                        "Illegal immutable type \"" +
                                this +
                                "\", it has super type because it is decorated by neither @" +
                                Entity.class.getName() +
                                " nor @" +
                                MappedSuperclass.class.getName()
                );
            }
        }

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
            ImmutableType superType,
            BiFunction<DraftContext, Object, Draft> draftFactory
    ) {
        this(((ClassBasedDeclarationContainer)kotlinClass).getJClass(), superType, draftFactory);
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
    public ImmutableType getSuperType() {
        return superType;
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
    public Set<ImmutableProp> getKeyProps() {
        return keyProps;
    }

    @NotNull
    @Override
    public Map<String, ImmutableProp> getProps() {
        Map<String, ImmutableProp> props = this.props;
        if (props == null) {
            if (superType == null) {
                props = declaredProps;
            } else {
                props = new LinkedHashMap<>(superType.getProps());
                for (ImmutableProp declaredProp : declaredProps.values()) {
                    ImmutableProp conflictProp = props.put(declaredProp.getName(), declaredProp);
                    if (conflictProp != null && conflictProp != ((ImmutablePropImpl)declaredProp).getOriginal()) {
                        throw new ModelException(
                                "The property \"" +
                                        declaredProp +
                                        "\" overrides property of super type, this is not allowed"
                        );
                    }
                }
            }
            this.props = Collections.unmodifiableMap(props);
        }
        return props;
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
    public ImmutableProp getProp(int id) {
        ImmutableProp[] arr = this.getPropArr();
        if (id < 1 || id >= arr.length) {
            throw new IllegalArgumentException(
                    "There is no property whose id is " + id + " in \"" + this + "\""
            );
        }
        return arr[id];
    }

    @NotNull
    private ImmutableProp[] getPropArr() {
        ImmutableProp[] arr = propArr;
        if (arr == null) {
            arr = new ImmutableProp[getProps().size() + 1];
            for (ImmutableProp prop : getProps().values()) {
                arr[prop.getId()] = prop;
            }
            propArr = arr;
        }
        return arr;
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

    void setDeclaredProps(Map<String, ImmutableProp> map) {
        this.declaredProps = Collections.unmodifiableMap(map);
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
        LogicalDeletedInfo superInfo = superType != null ? superType.getLogicalDeletedInfo() : null;
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
                    new ImmutablePropImpl(this, (ImmutablePropImpl) superInfo.getProp())
            );
        } else {
            logicalDeletedInfo = declaredLogicalDeletedInfo;
        }
    }

    void setKeyProps(Set<ImmutableProp> keyProps) {
        Set<ImmutableProp> set = new LinkedHashSet<>();
        for (ImmutableProp keyProp : keyProps) {
            if (keyProp.getDeclaringType() != this) {
                keyProp = getProp(keyProp.getName());
            }
            set.add(keyProp);
        }
        this.keyProps = Collections.unmodifiableSet(set);
    }

    @Override
    public String getMicroServiceName() {
        return microServiceName;
    }

    @Override
    public String getTableName(MetadataStrategy strategy) {
        return tableNameMap.computeIfAbsent(strategy, this::getTableName0);
    }

    private String getTableName0(MetadataStrategy strategy) {
        Table table = javaClass.getAnnotation(Table.class);
        String tableName = table != null ? table.name() : "";
        if (tableName.isEmpty()) {
            return strategy.getNamingStrategy().tableName(this);
        }
        return tableName;
    }

    @Override
    public IdGenerator getIdGenerator(MetadataStrategy strategy) {
        IdGenerator generator = idGeneratorMap.computeIfAbsent(
                strategy,
                it -> {
                    IdGenerator g = IdGenerators.of(this, it.getNamingStrategy());
                    return g != null ? g : NIL_ID_GENERATOR;
                }
        );
        return generator == NIL_ID_GENERATOR ? null : generator;
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

        private final ImmutableType superType;

        private final BiFunction<DraftContext, Object, Draft> draftFactory;

        private String idPropName;

        private String versionPropName;

        private String logicalDeletedPropName;

        private final List<String> keyPropNames = new ArrayList<>();

        private final Set<Integer> propIds;

        private final Map<String, PropBuilder> propBuilderMap = new LinkedHashMap<>();

        BuilderImpl(
                Class<?> javaClass,
                ImmutableType superType,
                BiFunction<DraftContext, Object, Draft> draftFactory
        ) {
            this.kotlinType = null;
            this.javaClass = javaClass;
            this.superType = superType;
            this.draftFactory = draftFactory;
            this.propIds = superType != null ?
                    superType.getProps().values().stream().map(ImmutableProp::getId).collect(Collectors.toSet()) :
                    new HashSet<>();
        }

        BuilderImpl(
                KClass<?> kotlinType,
                ImmutableType superType,
                BiFunction<DraftContext, Object, Draft> draftFactory
        ) {
            this.kotlinType = kotlinType;
            this.javaClass = ((ClassBasedDeclarationContainer)kotlinType).getJClass();
            this.superType = superType;
            this.draftFactory = draftFactory;
            this.propIds = superType != null ?
                    superType.getProps().values().stream().map(ImmutableProp::getId).collect(Collectors.toSet()) :
                    new HashSet<>();
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
            if (superType != null && superType.getIdProp() != null) {
                throw new IllegalStateException(
                        "Cannot set id property for type \"" +
                                javaClass.getName() +
                                "\" because there is an id property in the super type \"" +
                                superType.getJavaClass().getName() +
                                "\""
                );
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
            if (superType != null && superType.getVersionProp() != null) {
                throw new IllegalStateException(
                        "Cannot set version property for type \"" +
                                javaClass.getName() +
                                "\" because there is an version property in the super type \"" +
                                superType.getJavaClass().getName() +
                                "\""
                );
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
            if (superType != null && superType.getLogicalDeletedInfo() != null) {
                throw new IllegalStateException(
                        "Cannot set logical deleted property for type \"" +
                                javaClass.getName() +
                                "\" because there is an id property in the super type \"" +
                                superType.getJavaClass().getName() +
                                "\""
                );
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
            if (!propIds.add(id)) {
                throw new IllegalArgumentException(
                        "The property id \"" +
                                id +
                                "." +
                                name +
                                "\" is already exists in current type or the super type"
                );
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
            if (superType != null && superType.getProps().containsKey(name)) {
                throw new IllegalArgumentException(
                        "The property \"" +
                                javaClass.getName() +
                                "." +
                                name +
                                "\" is already exists in super type"
                );
            }
            propBuilderMap.put(
                    name,
                    new PropBuilder(
                            id,
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
        public ImmutableTypeImpl build() {

            ImmutableTypeImpl type = kotlinType != null ?
                    new ImmutableTypeImpl(kotlinType, superType, draftFactory) :
                    new ImmutableTypeImpl(javaClass, superType, draftFactory);

            Map<String, ImmutableProp> map = new LinkedHashMap<>();
            if (superType != null && !superType.isEntity() && javaClass.isAnnotationPresent(Entity.class)) {
                for (ImmutableProp prop : superType.getProps().values()) {
                    map.put(prop.getName(), new ImmutablePropImpl(type, (ImmutablePropImpl) prop));
                }
            }
            for (Map.Entry<String, PropBuilder> e : propBuilderMap.entrySet()) {
                map.put(e.getKey(), e.getValue().build(type));
            }
            type.setDeclaredProps(map);

            if (idPropName != null) {
                type.setIdProp(type.declaredProps.get(idPropName));
            } else if (type.superType != null) {
                ImmutableProp superIdProp = type.superType.getIdProp();
                if (superIdProp != null) {
                    type.setIdProp(superIdProp);
                }
            }

            if (versionPropName != null) {
                type.setVersionProp(type.declaredProps.get(versionPropName));
            } else if (type.superType != null) {
                type.setVersionProp(type.superType.getVersionProp());
            }

            if (logicalDeletedPropName != null) {
                type.setDeclaredLogicalDeletedInfo(LogicalDeletedInfo.of(type.declaredProps.get(logicalDeletedPropName)));
            } else {
                type.setDeclaredLogicalDeletedInfo(null);
            }

            Set<ImmutableProp> keyProps = type.superType != null ?
                    new LinkedHashSet<>(type.superType.getKeyProps()) :
                    new LinkedHashSet<>();
            for (String keyPropName : keyPropNames) {
                keyProps.add(type.declaredProps.get(keyPropName));
            }
            type.setKeyProps(keyProps);

            return type;
        }
    }

    private static class PropBuilder {
        final int id;
        final String name;
        final ImmutablePropCategory category;
        final Class<?> elementType;
        final boolean nullable;
        final Class<? extends Annotation> associationType;

        private PropBuilder(
                int id,
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
