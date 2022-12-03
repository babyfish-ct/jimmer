package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.impl.DatabaseIdentifiers;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EntityManager {

    private final Map<ImmutableType, ImmutableTypeInfo> map;

    private final Map<String, ImmutableType> tableNameTypeMap;

    public EntityManager(Class<?> ... classes) {
        this(Arrays.asList(classes));
    }

    public EntityManager(Collection<Class<?>> classes) {
        if (classes.isEmpty()) {
            throw new IllegalArgumentException("classes cannot be empty");
        }
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
                for (ImmutableProp prop : entityProps(type)) {
                    ImmutableType targetType = prop.getTargetType();
                    if (targetType != null && targetType.isEntity()) {
                        ImmutableTypeInfo targetInfo = map.get(targetType);
                        if (targetInfo == null) {
                            throw new IllegalArgumentException(
                                    "The target type \"" +
                                            targetType +
                                            "\" of the property \"" +
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
        this.map = map;
        this.tableNameTypeMap = createTableNameTypeMap();
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
            for (ImmutableType type : entityManager.getAllTypes()) {
                classes.add(type.getJavaClass());
            }
        }
        return new EntityManager(classes);
    }

    public Set<ImmutableType> getAllTypes() {
        return map.keySet();
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

    @Nullable
    public ImmutableType getTypeByTableName(String tableName) {
        String standardTableName = DatabaseIdentifiers.comparableIdentifier(tableName);
        return tableNameTypeMap.get(standardTableName);
    }

    @NotNull
    public ImmutableType getNonNullTypeByTableName(String tableName) {
        ImmutableType type = getTypeByTableName(tableName);
        if (type == null) {
            throw new IllegalArgumentException(
                    "The table \"" +
                            tableName +
                            "\" is not managed by current EntityManager"
            );
        }
        return type;
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

    private Map<String, ImmutableType> createTableNameTypeMap() {
        Map<String, ImmutableType> tableNameTypeMap = new HashMap<>();
        for (ImmutableType type : map.keySet()) {
            if (!type.isEntity()) {
                continue;
            }
            String tableName = DatabaseIdentifiers.comparableIdentifier(type.getTableName());
            ImmutableType oldType = tableNameTypeMap.put(tableName, type);
            if (oldType != null) {
                throw new IllegalArgumentException(
                        "Illegal entity manager, the table \"" +
                                tableName +
                                "\" is shared by both \"" +
                                oldType +
                                "\" and \"" +
                                type +
                                "\""
                );
            }
            tableNameTypeMap.put(tableName, type);
            for (ImmutableProp prop : entityProps(type)) {
                if (prop.getStorage() instanceof MiddleTable) {
                    AssociationType associationType = AssociationType.of(prop);
                    String associationTableName = DatabaseIdentifiers.comparableIdentifier(associationType.getTableName());
                    ImmutableType oldAssociationType = tableNameTypeMap.put(associationTableName, associationType);
                    if (oldAssociationType != null && !oldAssociationType.equals(associationType)) {
                        throw new IllegalArgumentException(
                                "Illegal mapping, the table \"" +
                                        associationTableName +
                                        "\" is shared by both \"" +
                                        oldAssociationType +
                                        "\" and \"" +
                                        associationType +
                                        "\""
                        );
                    }
                    tableNameTypeMap.put(associationTableName, associationType);
                }
            }
        }
        return tableNameTypeMap;
    }

    private static Collection<ImmutableProp> entityProps(ImmutableType type) {
        ImmutableType superType = type.getSuperType();
        if (superType != null && superType.isMappedSuperclass()) {
            return type.getProps().values();
        }
        return type.getDeclaredProps().values();
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
