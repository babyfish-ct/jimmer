package org.babyfish.jimmer.meta.impl;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutablePropCategory;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.ModelException;
import org.babyfish.jimmer.runtime.DraftContext;
import org.babyfish.jimmer.sql.meta.Column;
import org.babyfish.jimmer.sql.meta.IdGenerator;
import org.babyfish.jimmer.sql.meta.IdentityIdGenerator;
import org.babyfish.jimmer.sql.meta.SequenceIdGenerator;

import javax.persistence.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.BiFunction;

class ImmutableTypeImpl implements ImmutableType {

    private Class<?> javaClass;

    private ImmutableType superType;

    private BiFunction<DraftContext, Object, Draft> draftFactory;

    private Map<String, ImmutableProp> declaredProps = new LinkedHashMap<>();

    private Map<String, ImmutableProp> props;

    private Map<String, ImmutableProp> selectableProps;

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

        Table table = javaClass.getAnnotation(Table.class);
        tableName = table != null ? table.name() : "";
        if (tableName.isEmpty()) {
            tableName = Utils.databaseIdentifier(javaClass.getSimpleName());
        }
    }

    public Class<?> getJavaClass() {
        return javaClass;
    }

    public ImmutableType getSuperType() {
        return superType;
    }

    public BiFunction<DraftContext, Object, Draft> getDraftFactory() {
        return draftFactory;
    }

    public Map<String, ImmutableProp> getDeclaredProps() {
        return declaredProps;
    }

    public ImmutableProp getIdProp() {
        return idProp;
    }

    public ImmutableProp getVersionProp() {
        return versionProp;
    }

    public Set<ImmutableProp> getKeyProps() {
        return keyProps;
    }

    public String getTableName() {
        return tableName;
    }

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

    public ImmutableProp getProp(String name) {
        ImmutableProp prop = getProps().get(name);
        if (prop == null) {
            throw new IllegalArgumentException(
                    "There is no property \"" + name + "\" in \"" + this + "\""
            );
        }
        return prop;
    }

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

    void setIdProp(ImmutableProp idProp) {
        this.idProp = idProp;
        GeneratedValue generatedValue = idProp.getAnnotation(GeneratedValue.class);
        if (generatedValue == null) {
            return;
        }
        if (generatedValue.strategy() == GenerationType.AUTO) {
            String generator = generatedValue.generator();
            IdGenerator idGenerator = null;
            String error = null;
            Throwable errorCause = null;
            if (generator.isEmpty()) {
                error = "generator must be specified";
            } else {
                Class<?> idGeneratorType = null;
                try {
                    idGeneratorType = Class.forName(generator);
                } catch (ClassNotFoundException ex) {
                    error = "The class \"" + generator + "\" does not exists";
                }
                if (idGeneratorType != null) {
                    if (!IdGenerator.class.isAssignableFrom(idGeneratorType)) {
                        error = "the class \"" +
                                generator +
                                "\" does not implement \"" +
                                IdGenerator.class.getName() +
                                "\"";
                    }
                    try {
                        idGenerator = (IdGenerator) idGeneratorType.getDeclaredConstructor().newInstance();
                    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException ex) {
                        error = "cannot create the instance of \"" + generator + "\"";
                        errorCause = ex;
                    } catch (InvocationTargetException ex) {
                        error = "cannot create the instance of \"" + generator + "\"";
                        errorCause = ex.getTargetException();
                    }
                }
            }
            if (error != null) {
                throw new ModelException(
                        "Illegal property \"" + idProp + "\" with the annotation @GeneratedValue, " + error,
                        errorCause
                );
            }
            this.idGenerator = idGenerator;
        } else if (generatedValue.strategy() == GenerationType.IDENTITY) {
            this.idGenerator = IdentityIdGenerator.INSTANCE;
        } else if (generatedValue.strategy() == GenerationType.SEQUENCE) {
            String generator = generatedValue.generator();
            String sequenceName;
            if (generator.isEmpty()) {
                sequenceName = tableName + "_ID_SEQ";
            } else if (generator.startsWith(SEQUENCE_PREFIX)) {
                sequenceName = generator.substring(SEQUENCE_PREFIX.length());
            } else {
                SequenceGenerator seqGenerator = Arrays.stream(idProp.getAnnotations(SequenceGenerator.class))
                        .filter(it -> it.name().equals(generator))
                        .findFirst()
                        .orElseGet(null);
                if (seqGenerator == null) {
                    seqGenerator = Arrays.stream(javaClass.getAnnotationsByType(SequenceGenerator.class))
                            .filter(it -> it.name().equals(generator))
                            .findFirst()
                            .orElse(null);
                }
                if (seqGenerator == null) {
                    throw new ModelException(
                            "Illegal property \"" +
                                    idProp +
                                    "\"with annotation @GeneratedValue, " +
                                    "there is no sequence generator whose name is \"" +
                                    generator +
                                    "\""
                    );
                }
                sequenceName = seqGenerator.sequenceName();
            }
            if (sequenceName.isEmpty()) {
                sequenceName = tableName + "_ID_SEQ";
            }
            idGenerator = new SequenceIdGenerator(sequenceName);
        } else {
            throw new ModelException(
                    "Illegal property \"" + idProp + "\" with annotation @GeneratedValue, " +
                            "strategy \"" + generatedValue.strategy() + "\" is not supported"
            );
        }
    }

    void setVersionProp(ImmutableProp versionProp) {
        this.versionProp = versionProp;
    }

    void setKeyProps(Set<ImmutableProp> keyProps) {
        this.keyProps = Collections.unmodifiableSet(keyProps);
    }

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

        private List<String> keyPropNames = new ArrayList<>();

        BuilderImpl(
                Class<?> javaClass,
                ImmutableType superType,
                BiFunction<DraftContext, Object, Draft> draftFactory
        ) {
            this.type = new ImmutableTypeImpl(javaClass, superType, draftFactory);
        }

        public Builder id(String name, Class<?> elementType) {
            if (!type.javaClass.isAnnotationPresent(Entity.class)) {
                throw new IllegalStateException("Cannot set id for type that is not entity");
            }
            if (idPropName != null) {
                throw new IllegalStateException("id property has been set");
            }
            idPropName = name;
            return add(name, ImmutablePropCategory.SCALAR, elementType, false);
        }

        public Builder key(String name, Class<?> elementType) {
            if (!type.javaClass.isAnnotationPresent(Entity.class)) {
                throw new IllegalStateException("Cannot add key for type that is not entity");
            }
            keyPropNames.add(name);
            return add(name, ImmutablePropCategory.SCALAR, elementType, false);
        }

        public Builder version(String name) {
            if (!type.javaClass.isAnnotationPresent(Entity.class)) {
                throw new IllegalStateException("Cannot set version for type that is not entity");
            }
            if (versionPropName != null) {
                throw new IllegalStateException("version property has been set");
            }
            versionPropName = name;
            return add(name, ImmutablePropCategory.SCALAR, int.class, false);
        }

        public Builder add(
                String name,
                ImmutablePropCategory category,
                Class<?> elementType,
                boolean nullable
        ) {
            return add(
                    name,
                    category,
                    elementType,
                    nullable,
                    null
            );
        }

        public Builder add(
                String name,
                ImmutablePropCategory category,
                Class<?> elementType,
                boolean nullable,
                Class<? extends Annotation> associationType
        ) {
            validate();
            if (type.declaredProps.containsKey(name)) {
                throw new IllegalArgumentException(
                        "The property \"" +
                                type.javaClass.getName() +
                                "." +
                                name +
                                "\" is already exists"
                );
            }
            type.declaredProps.put(
                    name,
                    new ImmutablePropImpl(
                            type,
                            name,
                            category,
                            elementType,
                            nullable,
                            associationType
                    )
            );
            return this;
        }

        public ImmutableTypeImpl build() {
            validate();
            ImmutableTypeImpl type = this.type;
            type.declaredProps = Collections.unmodifiableMap(type.declaredProps);
            if (idPropName != null) {
                type.setIdProp(type.declaredProps.get(idPropName));
            } else if (type.superType != null) {
                type.setIdProp(type.superType.getIdProp());
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
