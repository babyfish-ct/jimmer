package org.babyfish.jimmer.meta.impl;

import kotlin.jvm.internal.ClassBasedDeclarationContainer;
import kotlin.reflect.KClass;
import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.meta.Column;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

class ImmutableTypeImpl implements ImmutableType {

    private final Class<?> javaClass;

    private final boolean isEntity;

    private final boolean isMappedSupperClass;

    private final Annotation immutableAnnotation;

    private KClass<?> kotlinClass;

    private final ImmutableType superType;

    private final BiFunction<DraftContext, Object, Draft> draftFactory;

    private Map<String, ImmutableProp> declaredProps = new LinkedHashMap<>();

    private Map<String, ImmutableProp> props;

    private ImmutableProp[] propArr;

    private Map<String, ImmutableProp> columnProps;

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
        this.javaClass = javaClass;
        this.superType = superType;
        this.draftFactory = draftFactory;

        this.isEntity = javaClass.isAnnotationPresent(Entity.class);
        this.isMappedSupperClass = javaClass.isAnnotationPresent(MappedSuperclass.class);

        Entity entity = javaClass.getAnnotation(Entity.class);
        MappedSuperclass mappedSuperclass = javaClass.getAnnotation(MappedSuperclass.class);
        if (entity != null && mappedSuperclass != null) {
            throw new ModelException(
                    "Illegal type \"" +
                            javaClass.getName() +
                            "\", it cannot be decorated by both @Entity and @MappedSuperclass"
            );
        }
        if (entity != null) {
            immutableAnnotation = entity;
        } else if (mappedSuperclass != null) {
            immutableAnnotation = mappedSuperclass;
        } else {
            immutableAnnotation = javaClass.getAnnotation(Immutable.class);
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
    public boolean isEntity() {
        return isEntity;
    }

    @Override
    public boolean isMappedSuperclass() {
        return isMappedSupperClass;
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
                    if (props.put(declaredProp.getName(), declaredProp) != null) {
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
    public ImmutableProp getPropByColumnName(String columnName) {
        String scName = DatabaseIdentifiers.standardIdentifier(columnName);
        ImmutableProp prop = getColumnProps().get(scName);
        if (prop == null) {
            throw new IllegalArgumentException(
                    "There is no property whose column name is \"" +
                            columnName +
                            "\" in type \"" +
                            this +
                            "\""
            );
        }
        return prop;
    }

    private Map<String, ImmutableProp> getColumnProps() {
        Map<String, ImmutableProp> cps = columnProps;
        if (cps == null) {
            cps = new LinkedHashMap<>();
            for (ImmutableProp prop : getProps().values()) {
                if (prop.getStorage() instanceof Column) {
                    String scName = DatabaseIdentifiers.standardIdentifier(prop.<Column>getStorage().getName());
                    ImmutableProp conflictProp = cps.put(scName, prop);
                    if (conflictProp != null) {
                        throw new ModelException(
                                "Conflict column name \"" +
                                        scName +
                                        "\" of \"" +
                                        conflictProp +
                                        "\" and \"" +
                                        prop +
                                        "\""
                        );
                    }
                }
            }
            columnProps = cps;
        }
        return cps;
    }

    @Override
    public Map<String, ImmutableProp> getSelectableProps() {
        Map<String, ImmutableProp> selectableProps = this.selectableProps;
        if (selectableProps == null) {
            selectableProps = new LinkedHashMap<>();
            selectableProps.put(getIdProp().getName(), getIdProp());
            for (ImmutableProp prop : getProps().values()) {
                if (!prop.isId() && prop.getStorage() instanceof Column) {
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
            selectableReferenceProps = new LinkedHashMap<>();
            for (ImmutableProp prop : getProps().values()) {
                if (prop.isReference(TargetLevel.ENTITY) && prop.getStorage() instanceof Column) {
                    selectableReferenceProps.put(prop.getName(), prop);
                }
            }
            this.selectableReferenceProps = Collections.unmodifiableMap(selectableReferenceProps);
        }
        return selectableReferenceProps;
    }

    void setIdProp(ImmutableProp idProp) {
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

    private static final String SEQUENCE_PREFIX = "sequence:";

    public static class BuilderImpl implements Builder {

        private ImmutableTypeImpl type;

        private String idPropName;

        private String versionPropName;

        private final List<String> keyPropNames = new ArrayList<>();

        private final Set<Integer> propIds;

        BuilderImpl(
                Class<?> javaClass,
                ImmutableType superType,
                BiFunction<DraftContext, Object, Draft> draftFactory
        ) {
            this.type = new ImmutableTypeImpl(javaClass, superType, draftFactory);
            this.propIds = superType != null ?
                    superType.getProps().values().stream().map(ImmutableProp::getId).collect(Collectors.toSet()) :
                    new HashSet<>();
        }

        BuilderImpl(
                KClass<?> kotlinType,
                ImmutableType superType,
                BiFunction<DraftContext, Object, Draft> draftFactory
        ) {
            this.type = new ImmutableTypeImpl(kotlinType, superType, draftFactory);
            this.propIds = superType != null ?
                    superType.getProps().values().stream().map(ImmutableProp::getId).collect(Collectors.toSet()) :
                    new HashSet<>();
        }

        @Override
        public Builder id(int id, String name, Class<?> elementType) {
            if (!type.javaClass.isAnnotationPresent(Entity.class)
                && !type.javaClass.isAnnotationPresent(MappedSuperclass.class)) {
                throw new IllegalStateException("Cannot set id for type that is not entity");
            }
            if (idPropName != null) {
                throw new IllegalStateException("id property has been set");
            }
            idPropName = name;
            return add(id, name, ImmutablePropCategory.SCALAR, elementType, false);
        }

        @Override
        public Builder key(int id, String name, Class<?> elementType) {
            if (!type.javaClass.isAnnotationPresent(Entity.class) &&
                !type.javaClass.isAnnotationPresent(MappedSuperclass.class)) {
                throw new IllegalStateException("Cannot add key for type that is not entity");
            }
            keyPropNames.add(name);
            return add(id, name, ImmutablePropCategory.SCALAR, elementType, false);
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
            if (!type.javaClass.isAnnotationPresent(Entity.class) &&
                !type.javaClass.isAnnotationPresent(MappedSuperclass.class)) {
                throw new IllegalStateException("Cannot set version for type that is not entity");
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
            validate();
            if (!propIds.add(id)) {
                throw new IllegalArgumentException(
                        "The property id \"" +
                                id +
                                "." +
                                name +
                                "\" is already exists in current type or the super type"
                );
            }
            if (type.declaredProps.containsKey(name)) {
                throw new IllegalArgumentException(
                        "The property \"" +
                                type.javaClass.getName() +
                                "." +
                                name +
                                "\" is already exists"
                );
            }
            if (type.superType != null && type.superType.getProps().containsKey(name)) {
                throw new IllegalArgumentException(
                        "The property \"" +
                                type.javaClass.getName() +
                                "." +
                                name +
                                "\" is already exists in super type"
                );
            }
            type.declaredProps.put(
                    name,
                    new ImmutablePropImpl(
                            type,
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

        @Override
        public ImmutableTypeImpl build() {
            validate();
            ImmutableTypeImpl type = this.type;
            type.declaredProps = Collections.unmodifiableMap(type.declaredProps);
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
            this.type = null;
            return type;
        }

        private void validate() {
            if (type == null) {
                throw new IllegalStateException("Current ImmutableType.Builder has been disposed");
            }
        }
    }
}
