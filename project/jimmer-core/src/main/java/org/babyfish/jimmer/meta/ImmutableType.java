package org.babyfish.jimmer.meta;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.meta.sql.Column;
import org.babyfish.jimmer.runtime.DraftContext;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class ImmutableType {

    private static Map<Class<?>, ImmutableType> positiveCacheMap =
            new WeakHashMap<Class<?>, ImmutableType>();

    private static Map<Class<?>, Void> negativeCacheMap =
            new LRUMap<>();

    private static ReadWriteLock cacheLock =
            new ReentrantReadWriteLock();

    private Class<?> javaClass;

    private ImmutableType superType;

    private BiFunction<DraftContext, Object, Draft> draftFactory;

    private Map<String, ImmutableProp> declaredProps = new LinkedHashMap<>();

    private Map<String, ImmutableProp> props;

    private Map<String, ImmutableProp> selectableProps;

    ImmutableProp idProp;

    private String tableName;

    ImmutableType(
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

    public static Builder newBuilder(
            Class<?> javaClass,
            ImmutableType superType,
            BiFunction<DraftContext, Object, Draft> draftFactory
    ) {
        return new Builder(javaClass, superType, draftFactory);
    }

    private static Class<?> getImmutableJavaClass(Class<?> javaClass) {
        boolean matched = Arrays.stream(javaClass.getAnnotations()).anyMatch(
                it -> it.annotationType() == Immutable.class ||
                        it.annotationType().getName().equals("javax.persistence.Entity")
        );
        if (matched) {
            return javaClass;
        }
        Class<?> superClass = javaClass.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            Class<?> immutableJavaClass = getImmutableJavaClass(superClass);
            if (immutableJavaClass != null) {
                return immutableJavaClass;
            }
        }
        for (Class<?> interfaceClass : javaClass.getInterfaces()) {
            Class<?> immutableJavaClass = getImmutableJavaClass(interfaceClass);
            if (immutableJavaClass != null) {
                return immutableJavaClass;
            }
        }
        return null;
    }

    public static ImmutableType tryGet(Class<?> javaClass) {

        ImmutableType immutableType;
        Lock lock;

        (lock = cacheLock.readLock()).lock();
        try {
            if (negativeCacheMap.containsKey(javaClass)) {
                return null;
            }
            immutableType = positiveCacheMap.get(javaClass);
        } finally {
            lock.unlock();
        }

        if (immutableType == null) {
            (lock = cacheLock.writeLock()).lock();
            try {
                if (negativeCacheMap.containsKey(javaClass)) {
                    return null;
                }
                immutableType = positiveCacheMap.get(javaClass);
                if (immutableType == null) {
                    immutableType = create(javaClass);
                    if (immutableType != null) {
                        positiveCacheMap.put(javaClass, immutableType);
                    } else {
                        negativeCacheMap.put(javaClass, null);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
        return immutableType;
    }

    private static ImmutableType create(Class<?> javaClass) {
        Class<?> immutableJavaClass = getImmutableJavaClass(javaClass);
        if (immutableJavaClass == null) {
            return null;
        }
        Class<?> draftClass;
        try {
            draftClass = Class.forName(immutableJavaClass.getName() + "Draft");
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException(
                    "Cannot find draft type for \"" + immutableJavaClass.getName() + "\""
            );
        }
        Class<?> producerClass = Arrays
                .stream(draftClass.getDeclaredClasses())
                .filter(it -> it.getSimpleName().equals("Producer"))
                .findFirst()
                .orElse(null);
        if (producerClass == null) {
            throw new IllegalArgumentException(
                    "Cannot find producer type for \"" + draftClass.getName() + "\""
            );
        }
        Field typeField;
        try {
            typeField = producerClass.getField("TYPE");
        } catch (NoSuchFieldException ex) {
            typeField = null;
        }
        if (typeField == null ||
                !Modifier.isPublic(typeField.getModifiers()) ||
                !Modifier.isStatic(typeField.getModifiers()) ||
                !Modifier.isFinal(typeField.getModifiers()) ||
                typeField.getType() != ImmutableType.class
        ) {
            throw new IllegalArgumentException(
                    "Illegal producer type \"" + producerClass.getName() + "\""
            );
        }
        try {
            return (ImmutableType) typeField.get(null);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Internal bug: Cannot access " + typeField);
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
                props.putAll(declaredProps);
            }
            this.props = props;
        }
        return props;
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
            this.selectableProps = selectableProps;
        }
        return selectableProps;
    }

    @Override
    public String toString() {
        return javaClass.getName();
    }

    public static class Builder {

        private ImmutableType type;

        private String idPropName;

        Builder(
                Class<?> javaClass,
                ImmutableType superType,
                BiFunction<DraftContext, Object, Draft> draftFactory
        ) {
            this.type = new ImmutableType(javaClass, superType, draftFactory);
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
                    new ImmutableProp(
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

        public ImmutableType build() {
            validate();
            ImmutableType type = this.type;
            type.declaredProps = Collections.unmodifiableMap(type.declaredProps);
            if (idPropName != null) {
                type.idProp = type.declaredProps.get(idPropName);
            } else if (type.superType != null) {
                type.idProp = type.superType.idProp;
            }
            this.type = null;
            return type;
        }

        private void validate() {
            if (type == null) {
                throw new IllegalStateException("Current ImmutableType.Builder has been disposed");
            }
        }
    }

    private static class LRUMap<K, V> extends LinkedHashMap<K, V> {

        LRUMap() {
            super(200, .75F, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return true;
        }
    }
}
