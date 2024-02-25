package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.lang.Lazy;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.ModelException;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.impl.AbstractImmutableTypeImpl;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.babyfish.jimmer.sql.meta.Storage;
import org.babyfish.jimmer.sql.meta.impl.DatabaseIdentifiers;
import org.babyfish.jimmer.sql.meta.impl.MetaCache;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class EntityManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityManager.class);

    private final ReadWriteLock reloadingLock = new ReentrantReadWriteLock();

    private volatile Data data;

    public EntityManager(Class<?> ... classes) {
        this(Arrays.asList(classes));
    }

    public EntityManager(Collection<Class<?>> classes) {
        if (!(classes instanceof Set<?>)) {
            classes = new LinkedHashSet<>(classes);
        }
        Set<String> qualifiedNames = new HashSet<>();
        for (Class<?> clazz : classes) {
            if (!qualifiedNames.add(clazz.getName())) {
                throw new IllegalArgumentException(
                        "Multiple classes with the same qualified name \"" +
                                clazz.getName() +
                                "\" but belonging to different class loaders " +
                                "cannot be registered into the entity manager"
                );
            }
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
                for (ImmutableType type : immutableType.getAllTypes()) {
                    map.put(type, new ImmutableTypeInfo());
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
                        if (otherType.getSuperTypes().contains(type)) {
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
        for (Map.Entry<ImmutableType, ImmutableTypeInfo> e : map.entrySet()) {
            if (!e.getKey().isEntity()) {
                continue;
            }
            List<ImmutableProp> props = new ArrayList<>();
            List<ImmutableProp> backProps = new ArrayList<>();
            boolean hasInverseLocalAssociation = false;
            for (ImmutableProp prop : e.getKey().getProps().values()) {
                if (prop.isRemote()) {
                    continue;
                }
                if (prop.getMappedBy() != null) {
                    hasInverseLocalAssociation = true;
                    continue;
                }
                if (prop.isMiddleTableDefinition()) {
                    props.add(prop);
                }
            }
            if (hasInverseLocalAssociation) {
                for (ImmutableProp backProp : e.getValue().backProps) {
                    if (backProp.getMappedBy() != null || backProp.isRemote()) {
                        continue;
                    }
                    if (backProp.isMiddleTableDefinition()) {
                        backProps.add(backProp);
                    } else if (backProp.isReference(TargetLevel.PERSISTENT) && backProp.isColumnDefinition()) {
                        backProps.add(backProp);
                    }
                }
            }
            if (!props.isEmpty() || !backProps.isEmpty()) {
                e.getValue().dissociationInfo = new DissociationInfo(
                        Collections.unmodifiableList(props),
                        Collections.unmodifiableList(backProps)
                );
            }
        }
        for (ImmutableTypeInfo info : map.values()) {
            info.implementationTypes = Collections.unmodifiableList(info.implementationTypes);
            info.directDerivedTypes = Collections.unmodifiableList(info.directDerivedTypes);
            info.allDerivedTypes = Collections.unmodifiableList(info.allDerivedTypes);
            info.backProps = Collections.unmodifiableList(
                    info
                            .backProps
                            .stream()
                            // sort is important, that means the order of cascade sql operations is fixed
                            .sorted(Comparator.comparing(ImmutableProp::toString))
                            .collect(Collectors.toList())
            );
        }
        map = Collections.unmodifiableMap(map);
        Map<String, ImmutableType> springDevToolMap = new HashMap<>((map.size() * 4 + 2) / 3);
        for (ImmutableType type : map.keySet()) {
            springDevToolMap.put(type.getJavaClass().getName(), type);
        }
        this.data = new Data(map, springDevToolMap);
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
            classLoader = Thread.currentThread().getContextClassLoader();
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
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to load resource \"" + url + "\"", ex);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load resources \"META-INF/jimmer/entities\"", ex);
        }
        return new EntityManager(classes);
    }

    public Set<ImmutableType> getAllTypes(String microServiceName) {
        if (microServiceName == null) {
            return data.map.keySet();
        }
        Set<ImmutableType> set = microServiceName.isEmpty() ?
                new LinkedHashSet<>((data.map.size() * 4 + 2) / 3) :
                new LinkedHashSet<>();
        for (ImmutableType type : data.map.keySet()) {
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

    @Nullable
    public DissociationInfo getDissociationInfo(ImmutableType type) {
        return info(type).dissociationInfo;
    }

    private ImmutableTypeInfo info(ImmutableType type) {
        ImmutableTypeInfo info = data.map.get(type);
        if (info == null) {
            ImmutableType oldType = data.typeMapForSpringDevTools.get(type.getJavaClass().getName());
            if (oldType != null) {
                LOGGER.info(
                        "You seem to be using spring-dev-tools (or other multi-ClassLoader technology), " +
                                "so that some entity metadata changes but the ORM's entire metadata graph " +
                                "is not updated, now try to reload the EntityManager."
                );
                try {
                    reload(type);
                } catch (RuntimeException | Error ex) {
                    throw new IllegalStateException(
                            "You seem to be using spring-dev-tools (or other multi-ClassLoader technology), " +
                                    "so that some entity metadata changes but the ORM's entire metadata graph " +
                                    "is not updated, jimmer try to reload the EntityManager but meet some problem.",
                            ex
                    );
                }
                info = data.map.get(type);
            }
            if (info == null) {
                throw new IllegalArgumentException(
                        "\"" + type + "\" is not managed by current EntityManager"
                );
            }
        }
        return info;
    }

    private void reload(ImmutableType immutableType) {

        Lock lock;

        (lock = reloadingLock.readLock()).lock();
        try {
            if (data.map.containsKey(immutableType)) {
                return;
            }
        } finally {
            lock.unlock();
        }

        (lock = reloadingLock.writeLock()).lock();
        try {
            if (data.map.containsKey(immutableType)) {
                return;
            }
            EntityManager newEntityManager = EntityManager.fromResources(
                    immutableType.getJavaClass().getClassLoader(),
                    null
            );
            data = newEntityManager.data;
        } finally {
            lock.unlock();
        }
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
        for (ImmutableType superType : type.getSuperTypes()) {
            if (superType.isMappedSuperclass()) {
                return true;
            }
        }
        return false;
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

        // For entity
        DissociationInfo dissociationInfo;
    }

    public boolean isActiveMiddleTableProp(ImmutableProp prop) {
        return data.activeMiddleTableProps.contains(prop);
    }

    public Map<List<Object>, ImmutableType> getTypeMapByServiceAndTable(
            String microServiceName,
            String tableName,
            MetadataStrategy strategy
    ) {
        Objects.requireNonNull(microServiceName, "`microServiceName` cannot be null");
        tableName = DatabaseIdentifiers.comparableIdentifier(
                Objects.requireNonNull(tableName, "`tableName` cannot be null")
        );
        while (true) {
            Map<List<Object>, ImmutableType> type = data.getTypeMap(strategy).get(new Key(microServiceName, tableName));
            if (type != null) {
                return type;
            }
            int index = tableName.indexOf('.');
            if (index == -1) {
                break;
            }
            tableName = tableName.substring(index + 1);
            if (tableName.isEmpty()) {
                break;
            }
        }
        return Collections.emptyMap();
    }

    public void validate(MetadataStrategy strategy) {
        data.getTypeMap(strategy);
    }

    private static void tableSharedBy(Key key, ImmutableType type1, ImmutableType type2, List<Object> filteredValues) {
        String microServiceDescription = key.microServiceName.isEmpty() ?
                "" :
                "in the microservice \"" + key.microServiceName + "\", ";
        if (type1 instanceof AssociationType && type2 instanceof AssociationType) {
            AssociationType associationType1 = (AssociationType) type1;
            AssociationType associationType2 = (AssociationType) type2;
            if (associationType1.getSourceType() == associationType2.getTargetType() &&
                    associationType1.getTargetType() == associationType2.getSourceType()) {
                throw new IllegalArgumentException(
                        "Illegal entity manager, " +
                                microServiceDescription +
                                "the table \"" +
                                key.tableName +
                                "\" is shared by both \"" +
                                type1 +
                                "\" and \"" +
                                type2 +
                                "\". These two associations seem to form a bidirectional association, " +
                                "if so, please make one of them real (using \"@" +
                                JoinTable.class.getName() +
                                "\") and the other image (specify `mappedBy` of @\"" +
                                OneToOne.class.getName() +
                                "\", \"@" + OneToMany.class.getName() +
                                "\" or \"" + ManyToMany.class.getName() +
                                "\")"
                );
            }
        }
        throw new IllegalArgumentException(
                "Illegal entity manager, " +
                        microServiceDescription +
                        "the table \"" +
                        key.tableName +
                        "\" is shared by both \"" +
                        type1 +
                        "\" and \"" +
                        type2 +
                        "\"" +
                        (filteredValues != null ? " with the same join table filtered values: " + filteredValues : "")
        );
    }

    private static boolean validateMiddleTableCompatibility(
            AssociationType type1,
            AssociationType type2,
            String microServiceName,
            MetadataStrategy strategy
    ) {
        MiddleTable middleTable1 = type1.getBaseProp().getStorage(strategy);
        MiddleTable middleTable2 = type2.getBaseProp().getStorage(strategy);
        Lazy<String> prefix = new Lazy<>(() ->
                "The middle table \"" +
                        middleTable1.getTableName() +
                        "\"" +
                        (microServiceName.isEmpty() ? "" : " in the microservice \"" + microServiceName + "\" ") +
                        "cannot be shared by \"" +
                        type1 +
                        "\" and \"" +
                        type2 +
                        "\" by different filter values, "
        );
        boolean sameColumns = middleTable1.getColumnDefinition().toColumnNames().equals(middleTable2.getColumnDefinition().toColumnNames()) &&
                middleTable1.getTargetColumnDefinition().toColumnNames().equals(middleTable2.getTargetColumnDefinition().toColumnNames());
        boolean inverseJoinColumns = middleTable1.getColumnDefinition().toColumnNames().equals(middleTable2.getTargetColumnDefinition().toColumnNames()) &&
                middleTable1.getTargetColumnDefinition().toColumnNames().equals(middleTable2.getColumnDefinition().toColumnNames());
        if (!sameColumns && !inverseJoinColumns) {
            throw new ModelException(
                    prefix.get() + "the foreign columns are not same"
            );
        }
        if ((middleTable1.getLogicalDeletedInfo() == null) != (middleTable2.getLogicalDeletedInfo() == null)) {
            throw new ModelException(
                    prefix.get() + "one of them declares the logical deleted filter and the other one does not"
            );
        }
        if (middleTable1.getLogicalDeletedInfo() != null) {
            if (!DatabaseIdentifiers.comparableIdentifier(middleTable1.getLogicalDeletedInfo().getColumnName()).equals(
                    DatabaseIdentifiers.comparableIdentifier(middleTable2.getLogicalDeletedInfo().getColumnName()))) {
                throw new ModelException(
                        prefix.get() +
                                "the logical deleted column of \"" +
                                type1 +
                                "\" is \"" +
                                middleTable1.getLogicalDeletedInfo().getColumnName() +
                                "\" but the logical deleted column of \"" +
                                type2 +
                                "\" is \"" +
                                middleTable2.getLogicalDeletedInfo().getColumnName() +
                                "\""
                );
            }
            if (middleTable1.getLogicalDeletedInfo().getType() != middleTable2.getLogicalDeletedInfo().getType()) {
                throw new ModelException(
                        prefix.get() +
                                "the type of the logical deleted column of \"" +
                                type1 +
                                "\" is \"" +
                                middleTable1.getLogicalDeletedInfo().getType().getName() +
                                "\" but the type of the logical deleted column of \"" +
                                type2 +
                                "\" is \"" +
                                middleTable2.getLogicalDeletedInfo().getType().getName() +
                                "\""
                );
            }
            if (middleTable1.getLogicalDeletedInfo().getGeneratorType() != middleTable2.getLogicalDeletedInfo().getGeneratorType()) {
                throw new ModelException(
                        prefix.get() +
                                "the generator type of the logical deleted column of \"" +
                                type1 +
                                "\" is \"" +
                                middleTable1.getLogicalDeletedInfo().getGeneratorType().getName() +
                                "\" but the generator type of the logical deleted column of \"" +
                                type2 +
                                "\" is \"" +
                                middleTable2.getLogicalDeletedInfo().getGeneratorType().getName() +
                                "\""
                );
            }
            if (!Objects.equals(middleTable1.getLogicalDeletedInfo().getGeneratorRef(), middleTable2.getLogicalDeletedInfo().getGeneratorRef())) {
                throw new ModelException(
                        prefix.get() +
                                "the generator ref of the logical deleted column of \"" +
                                type1 +
                                "\" is \"" +
                                middleTable1.getLogicalDeletedInfo().getGeneratorRef() +
                                "\" but the generator ref of the logical deleted column of \"" +
                                type2 +
                                "\" is \"" +
                                middleTable2.getLogicalDeletedInfo().getGeneratorRef() +
                                "\""
                );
            }
        }
        if (middleTable1.getFilterInfo() != null && middleTable2.getFilterInfo() != null) {
            if (!DatabaseIdentifiers.comparableIdentifier(middleTable1.getFilterInfo().getColumnName()).equals(
                    DatabaseIdentifiers.comparableIdentifier(middleTable2.getFilterInfo().getColumnName()))) {
                throw new ModelException(
                        prefix.get() +
                                "the filtered column of \"" +
                                type1 +
                                "\" is \"" +
                                middleTable1.getFilterInfo().getColumnName() +
                                "\" but the filtered columns of \"" +
                                type2 +
                                "\" is \"" +
                                middleTable2.getFilterInfo().getColumnName() +
                                "\""
                );
            }
            if (middleTable1.getFilterInfo().getType() != middleTable2.getFilterInfo().getType()) {
                throw new ModelException(
                        prefix.get() +
                                "the type of the filtered column of \"" +
                                type1 +
                                "\" is \"" +
                                middleTable1.getFilterInfo().getType().getName() +
                                "\" but the type of the filtered column of \"" +
                                type2 +
                                "\" is \"" +
                                middleTable2.getFilterInfo().getType().getName() +
                                "\""
                );
            }
        }
        return inverseJoinColumns;
    }

    private static class Key {

        final String microServiceName;

        final String tableName;

        private Key(String microServiceName, String tableName) {
            this.microServiceName = microServiceName;
            this.tableName = tableName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key key = (Key) o;
            return microServiceName.equals(key.microServiceName) && tableName.equals(key.tableName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(microServiceName, tableName);
        }

        @Override
        public String toString() {
            return "Key{" +
                    "microServiceName='" + microServiceName + '\'' +
                    ", tableName='" + tableName + '\'' +
                    '}';
        }
    }

    private static class Data {

        final Map<ImmutableType, ImmutableTypeInfo> map;

        final Map<String, ImmutableType> typeMapForSpringDevTools;

        final MetaCache<Map<Key, Map<List<Object>, ImmutableType>>> typeMapCache =
                new MetaCache<>(this::createTypeMap);

        final Set<ImmutableProp> activeMiddleTableProps = new HashSet<>();

        Data(Map<ImmutableType, ImmutableTypeInfo> map, Map<String, ImmutableType> typeMapForSpringDevTools) {
            this.map = map;
            this.typeMapForSpringDevTools = typeMapForSpringDevTools;
        }

        public Map<Key, Map<List<Object>, ImmutableType>> getTypeMap(MetadataStrategy strategy) {
            return typeMapCache.get(strategy);
        }

        @SuppressWarnings("unchecked")
        private Map<Key, Map<List<Object>, ImmutableType>> createTypeMap(MetadataStrategy strategy) {
            Map<Key, Map<List<Object>, ImmutableType>> typeMap = createRawTypeMap(strategy);
            for (Map.Entry<Key, Map<List<Object>, ImmutableType>> e : typeMap.entrySet()) {
                Map<List<Object>, ImmutableType> subMap = e.getValue();
                if (subMap.size() == 1) {
                    Map.Entry<List<Object>, ImmutableType> uniqueEntry = subMap.entrySet().iterator().next();
                    e.setValue(Collections.singletonMap(uniqueEntry.getKey(), uniqueEntry.getValue()));
                    activeAssociationType(uniqueEntry.getValue(), strategy);
                } else {
                    AssociationType associationType = (AssociationType) subMap.get(null);
                    if (associationType != null && !associationType.getBaseProp().<MiddleTable>getStorage(strategy).isReadonly()) {
                        throw new ModelException(
                                "Illegal property \"" +
                                        associationType.getBaseProp() +
                                        "\", its join table must be readonly because it is mixed result the associations " +
                                        subMap
                                                .entrySet()
                                                .stream()
                                                .filter(it -> it.getKey() != null)
                                                .map(it -> "\"" + ((AssociationType) it.getValue()).getBaseProp() + "\"")
                                                .collect(Collectors.joining(", "))
                        );
                    }
                    for (Map.Entry<List<Object>, ImmutableType> nestedEntry : subMap.entrySet()) {
                        List<Object> filteredValues = nestedEntry.getKey();
                        associationType = (AssociationType) nestedEntry.getValue();
                        if (filteredValues != null) {
                            if (filteredValues.size() == 1) {
                                activeAssociationType(nestedEntry.getValue(), strategy);
                            } else {
                                for (Object filteredValue : filteredValues) {
                                    AssociationType singleFilteredValueType = (AssociationType) subMap.get(Collections.singletonList(filteredValue));
                                    if (singleFilteredValueType == null) {
                                        throw new IllegalArgumentException(
                                                "Illegal property \"" +
                                                        associationType.getBaseProp() +
                                                        "\", it has multiple filtered values, \"" +
                                                        filteredValue +
                                                        "\" is one of them " +
                                                        "but there is no other association based on the same middle table " +
                                                        "with that single filter value."
                                        );
                                    }
                                }
                            }
                        }
                    }
                    e.setValue(Collections.unmodifiableMap(subMap));
                }
            }

            for (ImmutableType type : map.keySet()) {
                if (!type.isEntity()) {
                    continue;
                }
                String tableName = DatabaseIdentifiers.comparableIdentifier(type.getTableName(strategy));
                extendRawTypeMap(type.getMicroServiceName(), tableName, typeMap);
                for (ImmutableProp prop : type.getProps().values()) {
                    if (prop.isMiddleTableDefinition()) {
                        String middleTableName = DatabaseIdentifiers.comparableIdentifier(
                                prop.<MiddleTable>getStorage(strategy).getTableName()
                        );
                        extendRawTypeMap(type.getMicroServiceName(), middleTableName, typeMap);
                    }
                }
            }
            return typeMap;
        }

        private Map<Key, Map<List<Object>, ImmutableType>> createRawTypeMap(MetadataStrategy strategy) {
            Map<Key, Map<List<Object>, ImmutableType>> typeMap = new LinkedHashMap<>();
            for (ImmutableType type : map.keySet()) {
                if (!type.isEntity()) {
                    continue;
                }
                String tableName = DatabaseIdentifiers.comparableIdentifier(type.getTableName(strategy));
                int lastDotIndex = tableName.lastIndexOf('.');
                if (lastDotIndex != -1) {
                    tableName = tableName.substring(lastDotIndex + 1);
                }
                String microServiceName = type.getMicroServiceName();
                Key key = new Key(microServiceName, tableName);
                Map<List<Object>, ImmutableType> subTypeMap = typeMap.computeIfAbsent(key, it -> new LinkedHashMap<>());
                ImmutableType conflictType = subTypeMap.put(null, type);
                if (conflictType != null) {
                    tableSharedBy(key, conflictType, type, null);
                }
                for (ImmutableProp prop : type.getProps().values()) {
                    if (prop.isMiddleTableDefinition()) {
                        AssociationType associationType = AssociationType.of(prop);
                        MiddleTable middleTable = prop.getStorage(strategy);
                        List<Object> filteredValues = middleTable.getFilterInfo() != null ?
                                middleTable.getFilterInfo().getValues() :
                                null;
                        String middleTableName = DatabaseIdentifiers.comparableIdentifier(middleTable.getTableName());
                        lastDotIndex = middleTableName.lastIndexOf('.');
                        if (lastDotIndex != -1) {
                            middleTableName = middleTableName.substring(lastDotIndex + 1);
                        }
                        key = new Key(microServiceName, middleTableName);
                        subTypeMap = typeMap.computeIfAbsent(key, it -> new LinkedHashMap<>());
                        if (filteredValues != null) {
                            conflictType = subTypeMap.get(null);
                            if (conflictType != null && !(conflictType instanceof AssociationType)) {
                                tableSharedBy(key, conflictType, associationType, null);
                            }
                        }
                        conflictType = subTypeMap.get(filteredValues);
                        if (conflictType != null) {
                            tableSharedBy(key, conflictType, associationType, filteredValues);
                        }
                        if (!subTypeMap.isEmpty()) {
                            validateMiddleTableCompatibility(
                                    (AssociationType) subTypeMap.values().iterator().next(),
                                    associationType,
                                    microServiceName,
                                    strategy
                            );
                        }
                        subTypeMap.put(filteredValues, associationType);
                    }
                }
            }
            for (ImmutableType type : map.keySet()) {
                if (!type.isEntity()) {
                    continue;
                }
                ((AbstractImmutableTypeImpl)type).validateColumnUniqueness(strategy);
                for (ImmutableProp prop : type.getProps().values()) {
                    if (!prop.isNullable() && prop.isReference(TargetLevel.ENTITY) && !prop.isTransient()) {
                        Storage storage = prop.getStorage(strategy);
                        if (prop.isRemote()) {
                            throw new ModelException(
                                    "Illegal reference association property \"" +
                                            prop +
                                            "\", it must be nullable because it is remote association"
                            );
                        } else if (storage instanceof ColumnDefinition) {
                            boolean isForeignKey = ((ColumnDefinition) storage).isForeignKey();
                            if (!isForeignKey) {
                                throw new ModelException(
                                        "Illegal reference association property \"" +
                                                prop +
                                                "\", it must be nullable because it is based on FAKE foreign key"
                                );
                            }
                        } else if (storage instanceof MiddleTable) {
                            boolean isForeignKey = ((MiddleTable) storage).getTargetColumnDefinition().isForeignKey();
                            if (!isForeignKey) {
                                throw new ModelException(
                                        "Illegal reference association property \"" +
                                                prop +
                                                "\", it must be nullable because it is based on middle table " +
                                                "whose target column is FAKE foreign key"
                                );
                            }
                        }
                    }
                }
            }
            for (ImmutableType type : map.keySet()) {
                if (type.isEntity() && !type.getSuperTypes().isEmpty()) {
                    Map<String, ImmutableProp> superPropMap = new HashMap<>();
                    for (ImmutableType superType : type.getSuperTypes()) {
                        for (ImmutableProp superProp : superType.getProps().values()) {
                            ImmutableProp conflictProp = superPropMap.put(superProp.getName(), superProp);
                            if (conflictProp != null) {
                                Storage storage1 = conflictProp.getStorage(strategy);
                                Storage storage2 = superProp.getStorage(strategy);
                                if (!Objects.equals(storage1, storage2)) {
                                    throw new ModelException(
                                            "Illegal entity type \"" +
                                                    type +
                                                    "\", conflict super properties \"" +
                                                    conflictProp +
                                                    "\" and \"" +
                                                    superProp +
                                                    "\", the storage of the first one is " +
                                                    storage1 +
                                                    " but the storage of the second one is " +
                                                    storage2
                                    );
                                }
                            }
                        }
                    }
                }
            }
            return typeMap;
        }

        private void activeAssociationType(ImmutableType type, MetadataStrategy strategy) {
            if (!(type instanceof AssociationType)) {
                return;
            }
            ImmutableProp prop = ((AssociationType) type).getBaseProp();
            activeMiddleTableProps.add(prop);
            ImmutableProp opposite = prop.getOpposite();
            if (opposite != null) {
                activeMiddleTableProps.add(opposite);
            }
        }
    }

    private static void extendRawTypeMap(String microServiceName, String tableName, Map<Key, Map<List<Object>, ImmutableType>> map) {
        int lastDotIndex = tableName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return;
        }
        Map<List<Object>, ImmutableType> subMap = map.get(
                new Key(
                        microServiceName,
                        tableName.substring(lastDotIndex + 1)
                )
        );
        if (subMap == null) {
            return;
        }
        int index;
        while ((index = tableName.indexOf('.')) != -1) {
            map.put(new Key(microServiceName, tableName), subMap);
            tableName = tableName.substring(index + 1);
        }
    }
}
