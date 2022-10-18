package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.Entity;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import java.util.*;
import java.util.stream.Collectors;

public class EntityManager {

    private final Map<ImmutableType, ImmutableTypeInfo> map;

    public EntityManager(ClassLoader classLoader, String... packageNames) {
        this(scan(classLoader, packageNames));
    }

    public EntityManager(Class<?> ... classes) {
        this(Arrays.asList(classes));
    }

    public EntityManager(Collection<Class<?>> classes) {
        if (classes.isEmpty()) {
            throw new IllegalArgumentException("classes cannot be empty");
        }
        Map<ImmutableType, ImmutableTypeInfo> map = new LinkedHashMap<>();
        for (Class<?> clazz : classes) {
            if (clazz != null) {
                ImmutableType immutableType = ImmutableType.get(clazz);
                if (!immutableType.isEntity()) {
                    throw new IllegalArgumentException(
                            "\"" +
                                    immutableType +
                                    "\" is not entity"
                    );
                }
                map.put(immutableType, new ImmutableTypeInfo());
                immutableType = immutableType.getSuperType();
                while (immutableType != null && (immutableType.isEntity() || immutableType.isMappedSuperclass())) {
                    map.put(immutableType, new ImmutableTypeInfo());
                    immutableType = immutableType.getSuperType();
                }
            }
        }
        for (Map.Entry<ImmutableType, ImmutableTypeInfo> e : map.entrySet()) {
            ImmutableType type = e.getKey();
            ImmutableTypeInfo info = e.getValue();
            if (type.isMappedSuperclass()) {
                for (ImmutableType otherType : map.keySet()) {
                    if (isImplementationType(type, otherType)) {
                        info.implementationTypes.add(otherType);
                    }
                }
            } else {
                for (ImmutableType otherType : map.keySet()) {
                    if (type != otherType && type.isAssignableFrom(otherType)) {
                        info.allDerivedTypes.add(otherType);
                        if (type == otherType.getSuperType()) {
                            info.directDerivedTypes.add(otherType);
                        }
                    }
                }
            }
        }
        for (ImmutableTypeInfo info : map.values()) {
            info.implementationTypes = Collections.unmodifiableList(info.implementationTypes);
            info.directDerivedTypes = Collections.unmodifiableList(info.directDerivedTypes);
            info.allDerivedTypes = Collections.unmodifiableList(info.allDerivedTypes);
        }
        this.map = Collections.unmodifiableMap(map);
    }

    private static Collection<Class<?>> scan(
            ClassLoader classLoader,
            String... packageNames
    ) {
        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .addClassLoaders(classLoader)
                        .forPackages(packageNames)
        );
        List<Class<?>> entityTypes = reflections
                .getTypesAnnotatedWith(Entity.class)
                .stream()
                .filter(it -> it.isInterface() && it.isAnnotationPresent(Entity.class))
                .collect(Collectors.toList());
        if (entityTypes.isEmpty()) {
            throw new IllegalArgumentException(
                    "No entity class is found in the package \"" +
                            Arrays.toString(packageNames) +
                            "\" by the specified class loader"
            );
        }
        return entityTypes;
    }

    public Set<ImmutableType> getAllTypes() {
        return map.keySet();
    }

    public List<ImmutableType> getImplementationTypes(ImmutableType type) {
        ImmutableTypeInfo info = map.get(type);
        return info != null ? info.implementationTypes : Collections.emptyList();
    }

    public List<ImmutableType> getDirectDerivedTypes(ImmutableType type) {
        ImmutableTypeInfo info = map.get(type);
        return info != null ? info.directDerivedTypes : Collections.emptyList();
    }

    public List<ImmutableType> getAllDerivedTypes(ImmutableType type) {
        ImmutableTypeInfo info = map.get(type);
        return info != null ? info.allDerivedTypes : Collections.emptyList();
    }

    private boolean isImplementationType(ImmutableType mappedSuperClass, ImmutableType type) {
        if (!mappedSuperClass.isMappedSuperclass()) {
            throw new AssertionError("Internal bug");
        }
        if (!type.isEntity()) {
            return false;
        }
        if (!mappedSuperClass.isAssignableFrom(type)) {
            return false;
        }
        return type.getSuperType().isMappedSuperclass();
    }

    private static class ImmutableTypeInfo {

        // For mapped super class
        List<ImmutableType> implementationTypes = new ArrayList<>();

        // For entity
        List<ImmutableType> directDerivedTypes = new ArrayList<>();

        // For entity
        List<ImmutableType> allDerivedTypes = new ArrayList<>();
    }
}
