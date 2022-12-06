package org.babyfish.jimmer.meta.impl;

import kotlin.jvm.internal.ClassBasedDeclarationContainer;
import kotlin.reflect.KClass;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

class ImmutableTypeImpl implements ImmutableType {

    @SuppressWarnings("unchecked")
    private static final Class<? extends Annotation>[] SQL_ANNOTATION_TYPES = new Class[] {
            Entity.class,
            MappedSuperclass.class,
            Embeddable.class
    };

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

    private Map<String, List<ImmutableProp>> chainMap;

    private Map<String, ImmutableProp> selectableProps;

    private Map<String, ImmutableProp> selectableReferenceProps;

    private ImmutableProp idProp;

    private ImmutableProp versionProp;

    private Set<ImmutableProp> keyProps = Collections.emptySet();

    private IdGenerator idGenerator;

    private String tableName;

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
            if (!superType.isMappedSuperclass()) {
                throw new ModelException(
                        "Illegal immutable type \"" +
                                this +
                                "\", the super type \"" +
                                superType +
                                "\" is not decorated by @" +
                                MappedSuperclass.class.getName()
                );
            }
            if (!isEntity && !isMappedSupperClass) {
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

        Table table = javaClass.getAnnotation(Table.class);
        tableName = table != null ? table.name() : "";
        if (tableName.isEmpty()) {
            tableName = DatabaseIdentifiers.databaseIdentifier(javaClass.getSimpleName());
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

    @Override
    public Annotation getImmutableAnnotation() {
        return immutableAnnotation;
    }

    KClass<?> getKotlinClass() { return kotlinClass; }

    @Override
    public boolean isAssignableFrom(ImmutableType type) {
        return javaClass.isAssignableFrom(type.getJavaClass());
    }

    @Override
    public ImmutableType getSuperType() {
        return superType;
    }

    @Override
    public BiFunction<DraftContext, Object, Draft> getDraftFactory() {
        return draftFactory;
    }

    @Override
    public Map<String, ImmutableProp> getDeclaredProps() {
        return declaredProps;
    }

    @Override
    public ImmutableProp getIdProp() {
        return idProp;
    }

    @Override
    public ImmutableProp getVersionProp() {
        return versionProp;
    }

    @Override
    public Set<ImmutableProp> getKeyProps() {
        return keyProps;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

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
                    if (conflictProp != null && conflictProp != ((ImmutablePropImpl)declaredProp).getBase()) {
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

    @Override
    public List<ImmutableProp> getPropChainByColumnName(String columnName) {
        String cmpName = DatabaseIdentifiers.comparableIdentifier(columnName);
        List<ImmutableProp> chain = getChainMap().get(cmpName);
        if (chain == null) {
            throw new IllegalArgumentException(
                    "There is no property chain whose column name is \"" +
                            columnName +
                            "\" in type \"" +
                            this +
                            "\""
            );
        }
        return chain;
    }

    private Map<String, List<ImmutableProp>> getChainMap() {
        Map<String, List<ImmutableProp>> map = chainMap;
        if (map == null) {
            validateEntity();
            map = new LinkedHashMap<>();
            for (ImmutableProp prop : getProps().values()) {
                PropChains.addInto(prop, map);
            }
            chainMap = map;
        }
        return map;
    }

    @Override
    public Map<String, ImmutableProp> getSelectableProps() {
        Map<String, ImmutableProp> selectableProps = this.selectableProps;
        if (selectableProps == null) {
            selectableProps = new LinkedHashMap<>();
            selectableProps.put(getIdProp().getName(), getIdProp());
            for (ImmutableProp prop : getProps().values()) {
                if (!prop.isId() && prop.getStorage() instanceof ColumnDefinition) {
                    selectableProps.put(prop.getName(), prop);
                }
            }
            this.selectableProps = Collections.unmodifiableMap(selectableProps);
        }
        return selectableProps;
    }

    @Override
    public Map<String, ImmutableProp> getSelectableReferenceProps() {
        Map<String, ImmutableProp> selectableReferenceProps = this.selectableReferenceProps;
        if (selectableReferenceProps == null) {
            validateEntity();
            selectableReferenceProps = new LinkedHashMap<>();
            for (ImmutableProp prop : getProps().values()) {
                if (prop.isReference(TargetLevel.ENTITY) && prop.getStorage() instanceof ColumnDefinition) {
                    selectableReferenceProps.put(prop.getName(), prop);
                }
            }
            this.selectableReferenceProps = Collections.unmodifiableMap(selectableReferenceProps);
        }
        return selectableReferenceProps;
    }

    void setDeclaredProps(Map<String, ImmutableProp> map) {
        this.declaredProps = Collections.unmodifiableMap(map);
    }

    void setIdProp(ImmutableProp idProp) {
        if (idProp.isEmbedded(EmbeddedLevel.SCALAR)) {
            validateEmbeddedIdType(idProp.getTargetType(), null);
        }
        this.idProp = idProp;
        GeneratedValue generatedValue = idProp.getAnnotation(GeneratedValue.class);
        if (generatedValue == null) {
            return;
        }

        Class<? extends IdGenerator> generatorType = generatedValue.generatorType();

        GenerationType strategy = generatedValue.strategy();
        GenerationType strategyFromGeneratorType = GenerationType.AUTO;
        GenerationType strategyFromSequenceName = GenerationType.AUTO;

        if (UserIdGenerator.class.isAssignableFrom(generatorType)) {
            strategyFromGeneratorType = GenerationType.USER;
        } else if (IdentityIdGenerator.class.isAssignableFrom(generatorType)) {
            strategyFromGeneratorType = GenerationType.IDENTITY;
        } else if (SequenceIdGenerator.class.isAssignableFrom(generatorType)) {
            strategyFromGeneratorType = GenerationType.SEQUENCE;
        }

        if (!generatedValue.sequenceName().isEmpty()) {
            strategyFromSequenceName = GenerationType.SEQUENCE;
        }

        if (strategy != GenerationType.AUTO &&
                strategyFromGeneratorType != GenerationType.AUTO &&
                strategy != strategyFromGeneratorType) {
            throw new ModelException(
                    "Illegal property \"" +
                            idProp +
                            "\", it's decorated by the annotation @" +
                            GeneratedValue.class.getName() +
                            " but that annotation has conflict attributes 'strategy' and 'generatorType'"
            );
        }
        if (strategy != GenerationType.AUTO &&
                strategyFromSequenceName != GenerationType.AUTO &&
                strategy != strategyFromSequenceName) {
            throw new ModelException(
                    "Illegal property \"" +
                            idProp +
                            "\", it's decorated by the annotation @" +
                            GeneratedValue.class.getName() +
                            " but that annotation has conflict attributes 'strategy' and 'sequenceName'"
            );
        }
        if (strategyFromGeneratorType != GenerationType.AUTO &&
                strategyFromSequenceName != GenerationType.AUTO &&
                strategyFromGeneratorType != strategyFromSequenceName) {
            throw new ModelException(
                    "Illegal property \"" +
                            idProp +
                            "\", it's decorated by the annotation @" +
                            GeneratedValue.class.getName() +
                            " but that annotation has conflict attributes 'generatorType' and 'sequenceName'"
            );
        }

        if (strategy == GenerationType.AUTO) {
            strategy = strategyFromGeneratorType;
        }
        if (strategy == GenerationType.AUTO) {
            strategy = strategyFromSequenceName;
        }
        if (strategy == GenerationType.AUTO) {
            throw new ModelException(
                    "Illegal property \"" +
                            idProp +
                            "\", it's decorated by the annotation @" +
                            GeneratedValue.class.getName() +
                            " but that annotation does not have any attributes"
            );
        }

        if ((strategy == GenerationType.IDENTITY || strategy == GenerationType.SEQUENCE)) {
            Class<?> returnType = idProp.getElementClass();
            if (!returnType.isPrimitive() && !Number.class.isAssignableFrom(returnType)) {
                throw new ModelException(
                        "Illegal property \"" +
                                idProp +
                                "\", it's id generation strategy is \"" +
                                strategy +
                                "\", but that the type of id is not numeric"
                );
            }
        } else if (strategy == GenerationType.USER) {
            Class<?> returnType = idProp.getElementClass();
            Map<?, Type> typeArguments = TypeUtils.getTypeArguments(generatorType, UserIdGenerator.class);
            Class<?> parsedType = null;
            if (!typeArguments.isEmpty()) {
                Type type = typeArguments.values().iterator().next();
                if (type instanceof Class<?>) {
                    parsedType = (Class<?>) type;
                }
            }
            if (parsedType == null) {
                throw new ModelException(
                        "Illegal property \"" +
                                idProp +
                                "\", the generator type is \"" +
                                generatorType.getName() +
                                "\" does support type argument for \"" +
                                UserIdGenerator.class +
                                "\""
                );
            }
            if (parsedType != returnType) {
                throw new ModelException(
                        "Illegal property \"" +
                                idProp +
                                "\", the generator type is \"" +
                                generatorType.getName() +
                                "\" generates id whose type is \"" +
                                parsedType.getName() +
                                "\" but the property returns \"" +
                                returnType.getName() +
                                "\""
                );
            }
        }

        if (strategy == GenerationType.USER) {
            IdGenerator idGenerator = null;
            String error = null;
            Throwable errorCause = null;
            if (generatorType == IdGenerator.None.class) {
                error = "'generatorType' must be specified when 'strategy' is 'GenerationType.USER'";
            }
            try {
                idGenerator = generatorType.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException ex) {
                error = "cannot create the instance of \"" + generatorType.getName() + "\"";
                errorCause = ex;
            } catch (InvocationTargetException ex) {
                error = "cannot create the instance of \"" + generatorType.getName() + "\"";
                errorCause = ex.getTargetException();
            }
            if (error != null) {
                throw new ModelException(
                        "Illegal property \"" + idProp + "\" with the annotation @GeneratedValue, " + error,
                        errorCause
                );
            }
            this.idGenerator = idGenerator;
        } else if (strategy == GenerationType.IDENTITY) {
            this.idGenerator = IdentityIdGenerator.INSTANCE;
        } else if (strategy == GenerationType.SEQUENCE) {
            String sequenceName = generatedValue.sequenceName();
            if (sequenceName.isEmpty()) {
                sequenceName = tableName + "_ID_SEQ";
            }
            idGenerator = new SequenceIdGenerator(sequenceName);
        }
    }

    void setVersionProp(ImmutableProp versionProp) {
        this.versionProp = versionProp;
    }

    void setKeyProps(Set<ImmutableProp> keyProps) {
        this.keyProps = Collections.unmodifiableSet(keyProps);
    }

    @Override
    public IdGenerator getIdGenerator() {
        return idGenerator;
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
                throw new IllegalStateException("Cannot set id for type that is not entity or mapped super type");
            }
            if (idPropName != null) {
                throw new IllegalStateException("id property has been set");
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
        public Builder key(int id, String name, Class<?> elementType) {
            if (!javaClass.isAnnotationPresent(Entity.class) &&
                !javaClass.isAnnotationPresent(MappedSuperclass.class)) {
                throw new IllegalStateException("Cannot add key for type that is not entity or mapped super class");
            }
            keyPropNames.add(name);
            return add(id, name, category(elementType), elementType, false);
        }

        @Override
        public Builder keyReference(
                int id,
                String name,
                Class<?> elementType,
                boolean nullable
        ) {
            keyPropNames.add(name);
            return add(id, name, ImmutablePropCategory.REFERENCE, elementType, nullable, ManyToOne.class);
        }

        @Override
        public Builder version(int id, String name) {
            if (!javaClass.isAnnotationPresent(Entity.class) &&
                !javaClass.isAnnotationPresent(MappedSuperclass.class)) {
                throw new IllegalStateException("Cannot set version for type that is not entity or mapped super class");
            }
            if (versionPropName != null) {
                throw new IllegalStateException("version property has been set");
            }
            versionPropName = name;
            return add(id, name, ImmutablePropCategory.SCALAR, int.class, false);
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
            } else if (associationType == OneToMany.class || associationType == ManyToMany.class) {
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
