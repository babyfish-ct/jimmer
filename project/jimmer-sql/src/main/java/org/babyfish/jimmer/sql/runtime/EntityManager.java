package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;

public class EntityManager {

    private final Map<ImmutableType, ImmutableTypeInfo> map;

    public EntityManager(Class<?> ... classes) {
        this(Arrays.asList(classes));
    }

    public EntityManager(Collection<Class<?>> classes) {
        if (!(classes instanceof Set<?>)) {
            classes = new LinkedHashSet<>(classes);
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
                for (ImmutableProp prop : type.getProps().values()) {
                    ImmutableType targetType = prop.getTargetType();
                    if (targetType != null && targetType.isEntity() && !prop.isRemote()) {
                        ImmutableTypeInfo targetInfo = map.get(targetType);
                        if (targetInfo == null) {
                            throw new IllegalArgumentException(
                                    "The target type \"" +
                                            targetType +
                                            "\" of the non-remote property \"" +
                                            prop +
                                            "\" is not manged by the current entity manager"
                            );
                        }
                        targetInfo.backProps.add(prop);
                    }
                }
            }
        }
        for (ImmutableTypeInfo info : map.values()) {
            info.implementationTypes = Collections.unmodifiableList(info.implementationTypes);
            info.directDerivedTypes = Collections.unmodifiableList(info.directDerivedTypes);
            info.allDerivedTypes = Collections.unmodifiableList(info.allDerivedTypes);
            info.backProps = Collections.unmodifiableList(info.backProps);
        }
        this.map = Collections.unmodifiableMap(map);
    }

    public static EntityManager combine(EntityManager ... entityManagers) {
        if (entityManagers.length == 0) {
            throw new IllegalArgumentException("No entity managers");
        }
        if (entityManagers.length == 1) {
            return entityManagers[0];
        }
        Set<Class<?>> classes = new LinkedHashSet<>();
        for (EntityManager entityManager : entityManagers) {
            for (ImmutableType type : entityManager.getAllTypes(null)) {
                if (type.isEntity()) {
                    classes.add(type.getJavaClass());
                }
            }
        }
        return new EntityManager(classes);
    }

    public static EntityManager fromResources(
            @Nullable ClassLoader classLoader,
            @Nullable Predicate<Class<?>> predicate
    ) {
        if (classLoader == null) {
            classLoader = EntityManager.class.getClassLoader();
        }

        Set<Class<?>> classes = new LinkedHashSet<>();
        try {
            Enumeration<URL> urls = classLoader.getResources("META-INF/jimmer/entities");
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                    while (true) {
                        String className = reader.readLine();
                        if (className == null) {
                            break;
                        }
                        className = className.trim();
                        if (!className.isEmpty()) {
                            Class<?> clazz;
                            try {
                                clazz = Class.forName(className, true, classLoader);
                            } catch (ClassNotFoundException ex) {
                                throw new IllegalStateException(
                                        "Cannot parse class name \"" +
                                                className +
                                                "\" in \"META-INF/jimmer/entities\"",
                                        ex
                                );
                            }
                            if (predicate == null || predicate.test(clazz)) {
                                classes.add(clazz);
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load resources \"META-INF/jimmer/entities\"", ex);
        }
        return new EntityManager(classes);
    }

    public Set<ImmutableType> getAllTypes(String microServiceName) {
        if (microServiceName == null) {
            return map.keySet();
        }
        Set<ImmutableType> set = microServiceName.isEmpty() ?
                new LinkedHashSet<>((map.size() * 4 + 2) / 3) :
                new LinkedHashSet<>();
        for (ImmutableType type : map.keySet()) {
            if (type.getMicroServiceName().equals(microServiceName)) {
                set.add(type);
            }
        }
        return set;
    }

    public List<ImmutableType> getImplementationTypes(ImmutableType type) {
        return info(type).implementationTypes;
    }

    public List<ImmutableType> getDirectDerivedTypes(ImmutableType type) {
        return info(type).directDerivedTypes;
    }

    public List<ImmutableType> getAllDerivedTypes(ImmutableType type) {
        return info(type).allDerivedTypes;
    }

    public List<ImmutableProp> getAllBackProps(ImmutableType type) {
        return info(type).backProps;
    }

    private ImmutableTypeInfo info(ImmutableType type) {
        ImmutableTypeInfo info = map.get(type);
        if (info == null) {
            throw new IllegalArgumentException(
                    "\"" + type + "\" is not managed by current EntityManager"
            );
        }
        return info;
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

        // For entity
        List<ImmutableProp> backProps = new ArrayList<>();
    }
}
