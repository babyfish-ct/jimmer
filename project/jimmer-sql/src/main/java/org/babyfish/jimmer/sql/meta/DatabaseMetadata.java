package org.babyfish.jimmer.sql.meta;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DatabaseMetadata {

    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

    private final DatabaseNamingStrategy databaseNamingStrategy;

    private final EntityManager entityManager;

    private final String microServiceName;

    private final AutoForeignKeyPolicy autoForeignKeyPolicy;
    
    private final Map<ImmutableType, String> tableNameMap = new HashMap<>();

    private final Map<String, ImmutableType> typeMap = new HashMap<>();

    private final Map<ImmutableType, IdGenerator> idGeneratorMap = new HashMap<>();

    private final Map<ImmutableProp, Storage> storageMap = new HashMap<>();

    private final Map<ImmutableType, Map<String, List<ImmutableProp>>> propChainsMap = new HashMap<>();

    public DatabaseMetadata(
            @NotNull DatabaseNamingStrategy databaseNamingStrategy,
            @NotNull EntityManager entityManager,
            @NotNull String microServiceName,
            @NotNull AutoForeignKeyPolicy autoForeignKeyPolicy) {
        this.databaseNamingStrategy = Objects.requireNonNull(
                databaseNamingStrategy,
                "`databaseNamingStrategy` cannot be null"
        );
        this.entityManager = Objects.requireNonNull(
                entityManager,
                "`entityManager` cannot be null"
        );
        this.microServiceName = Objects.requireNonNull(
                microServiceName,
                "`microServiceName` cannot be null"
        );
        this.autoForeignKeyPolicy = Objects.requireNonNull(
                autoForeignKeyPolicy,
                "`autoForeignKeyPolicy` cannot be null"
        );
        for (ImmutableType type : entityManager.getAllTypes(microServiceName)) {
            if (!type.isEntity()) {
                continue;
            }
            initializeTableName(type);
            initializeIdGenerator(type);
            for (ImmutableProp prop : type.getProps().values()) {
                ImmutableProp mappedBy = prop.getMappedBy();
                if (mappedBy != null) {
                    Storage storage = lazyGetStorage(mappedBy);
                    if (storage instanceof MiddleTable) {
                        MiddleTable middleTable = (MiddleTable) storage;
                        AssociationType associationType = AssociationType.of(prop);
                        tableNameMap.put(associationType, middleTable.getTableName());
                        storageMap.put(associationType.getSourceProp(), middleTable.getTargetColumnDefinition());
                        storageMap.put(associationType.getTargetProp(), middleTable.getColumnDefinition());
                    }
                } else {
                    Storage storage = lazyGetStorage(prop);
                    if (storage instanceof MiddleTable) {
                        MiddleTable middleTable = (MiddleTable) storage;
                        AssociationType associationType = AssociationType.of(prop);
                        tableNameMap.put(associationType, middleTable.getTableName());
                        storageMap.put(associationType.getSourceProp(), middleTable.getColumnDefinition());
                        storageMap.put(associationType.getTargetProp(), middleTable.getTargetColumnDefinition());
                    }
                }
            }
            initializePropChainMap(type);
        }
        initializeTypeMap();
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public String getMicroServiceName() {
        return microServiceName;
    }

    public Set<ImmutableType> getEntityTypes() {
        return Collections.unmodifiableSet(tableNameMap.keySet());
    }

    public String getTableName(ImmutableType type) {
        return tableNameMap.get(type);
    }

    public IdGenerator getIdGenerator(ImmutableType type) {
        return idGeneratorMap.get(type);
    }

    @SuppressWarnings("unchecked")
    public <S extends Storage> S getStorage(ImmutableProp prop) {
        return (S)storageMap.get(prop);
    }

    public List<ImmutableProp> getPropChainByColumnName(ImmutableType type, String columnName) {
        List<ImmutableProp> chain = null;
        Map<String, List<ImmutableProp>> chainMap = propChainsMap.get(type);
        if (chainMap != null) {
            String cmpName = comparableIdentifier(columnName);
            chain = chainMap.get(cmpName);
        }
        if (chain == null) {
            throw new IllegalArgumentException(
                    "There is no property chain whose column name is \"" +
                            columnName +
                            "\" in type \"" +
                            type +
                            "\""
            );
        }
        return chain;
    }

    public MiddleTable getMiddleTable(AssociationType type) {
        ImmutableProp prop = type.getBaseProp();
        ImmutableProp mappedBy = prop.getMappedBy();
        if (mappedBy != null) {
            return ((MiddleTable) storageMap.get(mappedBy)).getInverse();
        }
        return (MiddleTable) storageMap.get(prop);
    }

    public MiddleTable getMiddleTable(ImmutableProp prop) {
        return (MiddleTable) storageMap.get(prop);
    }

    @Nullable
    public ImmutableType getTypeByTableName(String tableName) {
        String standardTableName = comparableIdentifier(tableName);
        return typeMap.get(standardTableName);
    }

    @NotNull
    public ImmutableType getNonNullTypeByTableName(String tableName) {
        ImmutableType type = getTypeByTableName(tableName);
        if (type == null) {
            throw new IllegalArgumentException(
                    "The table \"" +
                            tableName +
                            "\" of micro service \"" +
                            microServiceName +
                            "\" is not managed by current EntityManager"
            );
        }
        return type;
    }

    private Storage lazyGetStorage(ImmutableProp prop) {
        Storage storage = storageMap.get(prop);
        if (storage == null && !storageMap.containsKey(prop)) {
            storage = createStorage(prop);
            storageMap.put(prop, storage);
        }
        return storage;
    }

    private void initializeTableName(ImmutableType type) {
        Class<?> javaClass = type.getJavaClass();
        Table table = javaClass.getAnnotation(Table.class);
        String tableName = table != null ? table.name() : "";
        if (tableName.isEmpty()) {
            tableName = databaseNamingStrategy.tableName(type);
        }
        tableNameMap.put(type, tableName);
    }

    private void initializeIdGenerator(ImmutableType type) {

        ImmutableProp idProp = type.getIdProp();

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

        if (strategy == GenerationType.USER && strategyFromGeneratorType != GenerationType.USER) {
            throw new ModelException(
                    "Illegal property \"" +
                            idProp +
                            "\", its generator strategy is explicitly specified to \"USER\"," +
                            "but its generator type does not implement " +
                            UserIdGenerator.class.getName()
            );
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
                Type typeArgument = typeArguments.values().iterator().next();
                if (typeArgument instanceof Class<?>) {
                    parsedType = (Class<?>) typeArgument;
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
            if (!Classes.matches(parsedType, returnType)) {
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

        IdGenerator idGenerator = null;
        if (strategy == GenerationType.USER) {
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
        } else if (strategy == GenerationType.IDENTITY) {
            idGenerator = IdentityIdGenerator.INSTANCE;
        } else if (strategy == GenerationType.SEQUENCE) {
            String sequenceName = generatedValue.sequenceName();
            if (sequenceName.isEmpty()) {
                sequenceName = databaseNamingStrategy.sequenceName(idProp.getDeclaringType());
            }
            idGenerator = new SequenceIdGenerator(sequenceName);
        }
        if (idGenerator != null) {
            idGeneratorMap.put(type, idGenerator);
        }
    }

    private Storage createStorage(ImmutableProp prop) {
        if (!prop.hasStorage()) {
            return null;
        }
        Annotation annotation = prop.getAssociationAnnotation();
        if (annotation == null) {
            if (prop.isEmbedded(EmbeddedLevel.SCALAR)) {
                return new EmbeddedTree(prop).toEmbeddedColumns(databaseNamingStrategy);
            }
            org.babyfish.jimmer.sql.Column column = prop.getAnnotation(org.babyfish.jimmer.sql.Column.class);
            String columnName = column != null ? column.name() : "";
            if (columnName.isEmpty()) {
                columnName = databaseNamingStrategy.columnName(prop);
            }
            return new SingleColumn(columnName, false);
        }
        Storage storage = middleTable(prop, databaseNamingStrategy, false);
        if (storage == null) {
            storage = joinColumn(prop, databaseNamingStrategy, false);
        }
        if (storage == null) {
            if (prop.getAssociationAnnotation() instanceof ManyToMany) {
                storage = middleTable(prop, databaseNamingStrategy, true);
            } else {
                storage = joinColumn(prop, databaseNamingStrategy, true);
            }
        }
        return storage;
    }

    private ColumnDefinition joinColumn(
            ImmutableProp prop,
            DatabaseNamingStrategy databaseNamingStrategy,
            boolean force
    ) {
        JoinColumn joinColumn = prop.getAnnotation(JoinColumn.class);
        JoinColumns joinColumns = prop.getAnnotation(JoinColumns.class);
        if (joinColumn == null && joinColumns == null && !force) {
            return null;
        }
        JoinColumnObj[] columns = joinColumns != null ?
                JoinColumnObj.array(prop, false, joinColumns.value(), autoForeignKeyPolicy) :
                JoinColumnObj.array(prop, false, joinColumn, autoForeignKeyPolicy);
        ColumnDefinition definition;
        try {
            definition= joinDefinition(columns, prop.getTargetType());
        } catch (IllegalJoinColumnCount ex) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", it has " +
                            ex.actual +
                            " join column(s), but the referenced property \"" +
                            prop.getTargetType().getIdProp() +
                            "\" has " +
                            ex.expect +
                            " join column(s)"
            );
        } catch (NoReference ex) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", the `referencedColumnName` of join columns " +
                            "must be set when multiple join columns are used"
            );
        } catch (ReferenceNothing ex) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", the `referencedColumnName` \"" +
                            ex.ref +
                            "\" is illegal"
            );
        } catch (SourceConflict ex) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", conflict column name \"" +
                            ex.name +
                            "\" in several join columns"
            );
        } catch (TargetConflict ex) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", conflict referenced column name \"" +
                            ex.ref +
                            "\" in several join columns"
            );
        } catch (ForeignKeyConflict ex) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", conflict columns \"" +
                            ex.columnName1 +
                            "\" and \"" +
                            ex.columnName2 +
                            "\", their attribute `foreignKey` is different"
            );
        }
        if (definition != null) {
            return definition;
        }
        return new SingleColumn(
                databaseNamingStrategy.foreignKeyColumnName(prop),
                isForeignKey(prop, false, ForeignKeyType.AUTO, autoForeignKeyPolicy)
        );
    }

    private MiddleTable middleTable(
            ImmutableProp prop,
            DatabaseNamingStrategy databaseNamingStrategy,
            boolean force
    ) {
        JoinTable joinTable = prop.getAnnotation(JoinTable.class);
        if (joinTable == null && !force) {
            return null;
        }
        JoinColumnObj[] joinColumns;
        JoinColumnObj[] inverseJoinColumns;
        if (joinTable != null) {
            if (!joinTable.joinColumnName().isEmpty() && joinTable.joinColumns().length != 0) {
                throw new ModelException(
                        "Illegal property \"" +
                                prop +
                                "\", `joinColumnName` and `joinColumns` of `@" +
                                JoinTable.class.getName() +
                                "` cannot be specified at the same time"
                );
            }
            if (!joinTable.inverseJoinColumnName().isEmpty() && joinTable.inverseColumns().length != 0) {
                throw new ModelException(
                        "Illegal property \"" +
                                prop +
                                "\", `inverseJoinColumnName` and `inverseColumns` of `@" +
                                JoinTable.class.getName() +
                                "` cannot be specified at the same time"
                );
            }
            joinColumns = JoinColumnObj.array(prop, true, joinTable.joinColumnName(), autoForeignKeyPolicy);
            if (joinColumns == null) {
                joinColumns = JoinColumnObj.array(prop, true, joinTable.joinColumns(), autoForeignKeyPolicy);
            }
            inverseJoinColumns = JoinColumnObj.array(prop, false, joinTable.inverseJoinColumnName(), autoForeignKeyPolicy);
            if (inverseJoinColumns == null) {
                inverseJoinColumns = JoinColumnObj.array(prop, false, joinTable.inverseColumns(), autoForeignKeyPolicy);
            }
        } else {
            joinColumns = null;
            inverseJoinColumns = null;
        }
        ColumnDefinition definition;
        ColumnDefinition targetDefinition;
        boolean leftParsed = false;
        try {
            definition = joinDefinition(
                    joinColumns,
                    prop.getDeclaringType()
            );
            leftParsed = true;
            targetDefinition = joinDefinition(
                    inverseJoinColumns,
                    prop.getTargetType()
            );
        } catch (IllegalJoinColumnCount ex) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", there are " +
                            ex.actual +
                            " `" +
                            (leftParsed ? "inverseColumn(s)" : "joinColumn(s)") +
                            "`, but the id property \"" +
                            (leftParsed ? prop.getTargetType() : prop.getDeclaringType()).getIdProp() +
                            "\" has " +
                            ex.expect +
                            " column(s)"
            );
        } catch (NoReference ex) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", the `inverseJoinColumns` of `" +
                            (leftParsed ? "inverseColumns" : "joinColumns") +
                            "` must be specified when multiple `" +
                            (leftParsed ? "inverseColumns" : "joinColumns") +
                            "` are used"
            );
        } catch (ReferenceNothing ex) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", the `referencedColumnName` \"" +
                            ex.ref +
                            "\" of `" +
                            (leftParsed ? "inverseColumns" : "joinColumns") +
                            "` is illegal"
            );
        } catch (SourceConflict ex) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", conflict column name \"" +
                            ex.name +
                            "\" in several " +
                            (leftParsed ? "inverseColumns" : "joinColumns")
            );
        } catch (TargetConflict ex) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", conflict referenced column name \"" +
                            ex.ref +
                            "\" in several " +
                            (leftParsed ? "inverseColumns" : "joinColumns")
            );
        } catch (ForeignKeyConflict ex) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", conflict columns \"" +
                            ex.columnName1 +
                            "\" and \"" +
                            ex.columnName2 +
                            "\" in " +
                            (leftParsed ? "inverseColumns" : "joinColumns") +
                            ", their attribute `foreignKey` is different"
            );
        }
        String tableName = joinTable != null ? joinTable.name() : "";
        if (tableName.isEmpty()) {
            tableName = databaseNamingStrategy.middleTableName(prop);
        }

        if (definition == null) {
            definition = new SingleColumn(
                    databaseNamingStrategy.middleTableBackRefColumnName(prop),
                    isForeignKey(prop, true, ForeignKeyType.AUTO, autoForeignKeyPolicy)
            );
        }
        if (targetDefinition == null) {
            targetDefinition = new SingleColumn(
                    databaseNamingStrategy.middleTableTargetRefColumnName(prop),
                    isForeignKey(prop, false, ForeignKeyType.AUTO, autoForeignKeyPolicy)
            );
        }
        return new MiddleTable(tableName, definition, targetDefinition);
    }

    private ColumnDefinition joinDefinition(
            JoinColumnObj[] joinColumns,
            ImmutableType targetType
    ) throws IllegalJoinColumnCount, NoReference, ReferenceNothing, TargetConflict, SourceConflict, ForeignKeyConflict {
        if (joinColumns == null || joinColumns.length == 0) {
            ColumnDefinition definition = (ColumnDefinition) lazyGetStorage(targetType.getIdProp());
            if (definition.size() == 1) {
                return null;
            }
            throw new IllegalJoinColumnCount(definition.size(), 0);
        }
        JoinColumnObj firstJoinColumn = null;
        for (JoinColumnObj joinColumn : joinColumns) {
            if (firstJoinColumn == null) {
                firstJoinColumn = joinColumn;
            } else if (firstJoinColumn.isForeignKey != joinColumn.isForeignKey) {
                throw new ForeignKeyConflict(firstJoinColumn.name, joinColumn.name);
            }
        }
        ColumnDefinition targetIdDefinition = (ColumnDefinition) lazyGetStorage(targetType.getIdProp());
        if (joinColumns.length != targetIdDefinition.size()) {
            throw new IllegalJoinColumnCount(targetIdDefinition.size(), joinColumns.length);
        }
        if (joinColumns.length == 1) {
            if (joinColumns[0].name.isEmpty()) {
                return null;
            }
            String ref = joinColumns[0].referencedColumnName;
            if (!ref.isEmpty() && !ref.equals(targetIdDefinition.name(0))) {
                throw new ReferenceNothing(ref);
            }
            return new SingleColumn(
                    joinColumns[0].name,
                    joinColumns[0].isForeignKey
            );
        }
        Map<String, String> columnMap = new HashMap<>();
        for (JoinColumnObj joinColumn : joinColumns) {
            String ref = joinColumn.referencedColumnName;
            if (ref.isEmpty()) {
                throw new NoReference();
            }
            if (targetIdDefinition.index(ref) == -1) {
                throw new ReferenceNothing(ref);
            }
            if (columnMap.put(ref, joinColumn.name) != null) {
                throw new TargetConflict(ref);
            }
        }
        Map<String, String> referencedColumnMap = new LinkedHashMap<>();
        for (String targetColumnName : targetIdDefinition) {
            String name = columnMap.get(targetColumnName);
            if (referencedColumnMap.put(name, targetColumnName) != null) {
                throw new SourceConflict(name);
            }
        }
        return new MultipleJoinColumns(
                referencedColumnMap,
                targetType.getIdProp().isEmbedded(EmbeddedLevel.SCALAR),
                joinColumns[0].isForeignKey
        );
    }

    private static class JoinColumnObj {

        final String name;

        final String referencedColumnName;

        final boolean isForeignKey;

        JoinColumnObj(String name, String referencedColumnName, boolean isForeignKey) {
            this.name = name;
            this.referencedColumnName = referencedColumnName;
            this.isForeignKey = isForeignKey;
        }

        static JoinColumnObj[] array(
                ImmutableProp prop,
                boolean backRef,
                String name,
                AutoForeignKeyPolicy autoForeignKeyPolicy
        ) {
            if (name.isEmpty()) {
                return null;
            }
            return new JoinColumnObj[] {
                    new JoinColumnObj(
                            name,
                            "",
                            isForeignKey(prop, backRef, ForeignKeyType.AUTO, autoForeignKeyPolicy)
                    )
            };
        }

        static JoinColumnObj[] array(
                ImmutableProp prop,
                boolean backRef,
                JoinColumn joinColumn,
                AutoForeignKeyPolicy autoForeignKeyPolicy
        ) {
            if (joinColumn == null) {
                return null;
            }
            return new JoinColumnObj[] {
                    new JoinColumnObj(
                            joinColumn.name(),
                            joinColumn.referencedColumnName(),
                            isForeignKey(prop, backRef, joinColumn.foreignKeyType(), autoForeignKeyPolicy)
                    )
            };
        }

        static JoinColumnObj[] array(
                ImmutableProp prop,
                boolean backRef,
                JoinColumn[] arr,
                AutoForeignKeyPolicy autoForeignKeyPolicy
        ) {
            if (arr.length == 0) {
                return null;
            }
            return Arrays.stream(arr).map(it ->
                new JoinColumnObj(
                        it.name(),
                        it.referencedColumnName(),
                        isForeignKey(prop, backRef, it.foreignKeyType(), autoForeignKeyPolicy)
                )
            ).toArray(JoinColumnObj[]::new);
        }
    }

    private static class IllegalJoinColumnCount extends Exception {

        final int expect;

        final int actual;

        private IllegalJoinColumnCount(int expect, int actual) {
            this.expect = expect;
            this.actual = actual;
        }
    }

    private static class NoReference extends Exception {}

    private static class ReferenceNothing extends Exception {

        final String ref;

        ReferenceNothing(String ref) {
            this.ref = ref;
        }
    }

    private static class TargetConflict extends Exception {

        final String ref;

        TargetConflict(String ref) {
            this.ref = ref;
        }
    }

    private static class SourceConflict extends Exception {

        final String name;

        SourceConflict(String name) {
            this.name = name;
        }
    }

    private static class ForeignKeyConflict extends Exception {

        final String columnName1;

        final String columnName2;

        private ForeignKeyConflict(String columnName1, String columnName2) {
            this.columnName1 = columnName1;
            this.columnName2 = columnName2;
        }
    }

    private static class EmbeddedTree {

        private final EmbeddedTree parent;

        private final ImmutableProp prop;

        private final String path;

        private final int depth;

        private final Map<String, EmbeddedTree> childMap;

        private OverrideContext usedCtx;

        public EmbeddedTree(ImmutableProp prop) {
            this(null, prop);
            applyOverride();
        }

        private EmbeddedTree(EmbeddedTree parent, ImmutableProp prop) {
            for (EmbeddedTree p = parent; p != null; p = p.parent) {
                if (p.prop.getDeclaringType() == prop.getTargetType()) {
                    List<String> names = new LinkedList<>();
                    for (EmbeddedTree p2 = parent; p2 != null; p2 = p2.parent) {
                        names.add(0, p2.prop.getName());
                        if (p2 == p) {
                            break;
                        }
                    }
                    throw new ModelException(
                            "Reference cycle is found in \"" +
                                    p.prop.getDeclaringType() +
                                    '.' +
                                    names.stream().collect(Collectors.joining(".")) +
                                    '.' +
                                    prop.getName() +
                                    "\""
                    );
                }
            }
            this.parent = parent;
            this.prop = prop;
            if (parent == null) {
                this.path = "";
                depth = 0;
            } else {
                String parentPath = parent.path;
                if (parentPath.isEmpty()) {
                    this.path = prop.getName();
                } else {
                    this.path = parentPath + '.' + prop.getName();
                }
                depth = parent.depth + 1;
            }
            if (prop.isEmbedded(EmbeddedLevel.SCALAR)) {
                Map<String, EmbeddedTree> map = new LinkedHashMap<>();
                for (ImmutableProp childProp : prop.getTargetType().getProps().values()) {
                    map.put(childProp.getName(), new EmbeddedTree(this, childProp));
                }
                this.childMap = map;
            } else {
                this.childMap = Collections.emptyMap();
            }
        }

        private void applyOverride() {
            PropOverrides propOverrides = prop.getAnnotation(PropOverrides.class);
            if (propOverrides != null) {
                for (PropOverride propOverride : propOverrides.value()) {
                    applyOverride(propOverride.prop(), new OverrideContext(prop, depth, propOverride));
                }
            }
            PropOverride propOverride = prop.getAnnotation(PropOverride.class);
            if (propOverride != null) {
                applyOverride(propOverride.prop(), new OverrideContext(prop, depth, propOverride));
            }
            for (EmbeddedTree childTree : childMap.values()) {
                childTree.applyOverride();
            }
        }

        private void applyOverride(String path, OverrideContext ctx) {
            String propName;
            String rest;
            int index = path.indexOf('.');
            if (index == -1) {
                propName = path;
                rest = null;
            } else {
                propName = path.substring(0, index);
                rest = path.substring(index + 1);
            }
            EmbeddedTree childTree = childMap.get(propName);
            if (childTree == null) {
                throw new ModelException(
                        "Illegal property \"" +
                                ctx.prop +
                                "\", the path \"" +
                                ctx.annotation.prop() +
                                "\" of `@PropOverride` is illegal, there is no property \"" +
                                propName +
                                "\" declared in \"" +
                                prop.getDeclaringType() +
                                "\""
                );
            }
            boolean tooShort = rest == null && childTree.prop.isEmbedded(EmbeddedLevel.SCALAR);
            boolean tooLong = rest != null && !childTree.prop.isEmbedded(EmbeddedLevel.SCALAR);
            if (tooLong || tooShort) {
                throw new ModelException(
                        "Illegal property \"" +
                                ctx.prop +
                                "\", the property path \"" +
                                ctx.annotation.prop() +
                                "\" of `@PropOverride` is too " +
                                (tooLong ? "long" : "short")
                );
            }
            if (rest == null) {
                childTree.useOverride(ctx);
            } else {
                childTree.applyOverride(rest, ctx);
            }
        }

        private void useOverride(OverrideContext ctx) {
            if (usedCtx == null || ctx.depth < usedCtx.depth) {
                usedCtx = ctx;
            } else if (this.usedCtx.depth == ctx.depth) {
                throw new ModelException(
                        "Illegal property \"" +
                                ctx.prop +
                                "\", the property path \"" +
                                ctx.annotation.prop() +
                                "\" and \"" +
                                usedCtx.annotation.prop() +
                                "\" of `@PropOverride`s are conflict"
                );
            }
        }

        public EmbeddedColumns toEmbeddedColumns(DatabaseNamingStrategy databaseNamingStrategy) {
            CollectContext ctx = new CollectContext(prop, databaseNamingStrategy);
            collect(ctx);
            return ctx.toEmbeddedColumns();
        }

        private void collect(CollectContext ctx) {
            ctx.accept(this);
            for (EmbeddedTree childTree : childMap.values()) {
                childTree.collect(ctx);
            }
        }

        private static class OverrideContext {

            final ImmutableProp prop;

            final int depth;

            final PropOverride annotation;

            private OverrideContext(ImmutableProp prop, int depth, PropOverride annotation) {
                this.prop = prop;
                this.depth = depth;
                this.annotation = annotation;
            }
        }

        private static class CollectContext {

            private final ImmutableProp prop;

            private final DatabaseNamingStrategy databaseNamingStrategy;

            private final Map<String, String> identifierPathMap = new LinkedHashMap<>();

            private final Map<String, EmbeddedColumns.PathData> pathMap = new LinkedHashMap<>();

            private CollectContext(ImmutableProp prop, DatabaseNamingStrategy strategy) {
                this.prop = prop;
                databaseNamingStrategy = strategy;
            }

            public void accept(EmbeddedTree tree) {
                if (tree.childMap.isEmpty()) {
                    String columnName = tree.usedCtx != null ?
                            tree.usedCtx.annotation.columnName() :
                            databaseNamingStrategy.columnName(tree.prop);
                    String comparableIdentifier = comparableIdentifier(columnName);
                    String conflictPath = identifierPathMap.put(comparableIdentifier, tree.path);
                    if (conflictPath != null) {
                        throw new ModelException(
                                "The property \"" +
                                        prop +
                                        "\" is illegal, its an embedded property but " +
                                        "both the path `" +
                                        conflictPath +
                                        "` and `" +
                                        tree.path +
                                        "` has been mapped to an same column \"" +
                                        columnName +
                                        "\""
                        );
                    }
                    for (EmbeddedTree t = tree; t != null; t = t.parent) {
                        boolean isTerminal = tree == t;
                        pathMap.computeIfAbsent(t.path, it -> new EmbeddedColumns.PathData(isTerminal)).columnNames.add(columnName);
                    }
                }
            }

            public EmbeddedColumns toEmbeddedColumns() {
                return new EmbeddedColumns(pathMap);
            }
        }
    }

    private void initializePropChainMap(ImmutableType type) {
        Map<String, List<ImmutableProp>> map = new LinkedHashMap<>();
        PropChains propChains = new PropChains();
        for (ImmutableProp prop : type.getProps().values()) {
            propChains.addInto(prop, map);
            if (prop.isMiddleTableDefinition()) {
                AssociationType associationType = AssociationType.of(prop);
                initializePropChainMap(associationType);
            }
            ImmutableProp mappedBy = prop.getMappedBy();
            if (mappedBy != null && mappedBy.isMiddleTableDefinition()) {
                AssociationType associationType = AssociationType.of(prop);
                initializePropChainMap(associationType);
            }
        }
        propChainsMap.put(type, map);
    }

    private void initializeTypeMap() {
        for (Map.Entry<ImmutableType, String> e : tableNameMap.entrySet()) {
            ImmutableType type = e.getKey();
            if (type instanceof AssociationType && ((AssociationType)type).getBaseProp().getMappedBy() != null) {
                continue;
            }
            String tableName = comparableIdentifier(e.getValue());
            ImmutableType oldType = typeMap.put(tableName, type);
            if (oldType != null && !oldType.equals(type)) {
                tableSharedBy(tableName, oldType, type);
            }
            for (ImmutableProp prop : type.getProps().values()) {
                if (prop.isMiddleTableDefinition()) {
                    AssociationType associationType = AssociationType.of(prop);
                    String associationTableName = comparableIdentifier(
                            ((MiddleTable)lazyGetStorage(prop)).getTableName()
                    );
                    oldType = typeMap.put(associationTableName, associationType);
                    if (oldType != null && !oldType.equals(associationType)) {
                        tableSharedBy(tableName, oldType, associationType);
                    }
                }
            }
        }
    }

    private static void tableSharedBy(String tableName, ImmutableType type1, ImmutableType type2) {
        if (type1 instanceof AssociationType && type2 instanceof AssociationType) {
            AssociationType associationType1 = (AssociationType) type1;
            AssociationType associationType2 = (AssociationType) type2;
            if (associationType1.getSourceType() == associationType2.getTargetType() &&
                    associationType1.getTargetType() == associationType2.getSourceType()) {
                throw new IllegalArgumentException(
                        "Illegal entity manager, the table \"" +
                                tableName +
                                "\" is shared by both \"" +
                                type1 +
                                "\" and \"" +
                                type2 +
                                "\". These two associations seem to form a bidirectional association, " +
                                "if so, please make one of them real (using @" +
                                JoinTable.class +
                                ") and the other image (specify `mappedBy` of @" +
                                ManyToOne.class +
                                ")"
                );
            }
        }
        throw new IllegalArgumentException(
                "Illegal entity manager, the table \"" +
                        tableName +
                        "\" is shared by both \"" +
                        type1 +
                        "\" and \"" +
                        type2 +
                        "\""
        );
    }

    private static boolean isForeignKey(
            ImmutableProp prop,
            boolean backRef,
            ForeignKeyType foreignKeyType,
            AutoForeignKeyPolicy autoForeignKeyPolicy
    ) {
        switch (foreignKeyType) {
            case REAL:
                if (autoForeignKeyPolicy == AutoForeignKeyPolicy.FORCED_FAKE) {
                    throw new ModelException(
                            "Illegal property \"" +
                                    prop +
                                    "\", the `foreignKeyType` of any @JoinColumn " +
                                    "cannot be `REAL` because this current database dialect " +
                                    "does not support foreign key constraint"
                    );
                }
                if (!backRef && prop.isRemote()) {
                    throw new ModelException(
                            "Illegal property \"" +
                                    prop +
                                    "\", the `foreignKeyType` of the @JoinColumn pointing to target type " +
                                    "cannot be `REAL` because this property is remote association across microservices"
                    );
                }
                return true;
            case FAKE:
                return false;
            default:
                return autoForeignKeyPolicy == AutoForeignKeyPolicy.REAL && (backRef || !prop.isRemote());
        }
    }

    public static String comparableIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        boolean cut = false;
        if (identifier.startsWith("`") && identifier.endsWith("`") && identifier.length() > 2) {
            cut = true;
        } else if (identifier.startsWith("\"") && identifier.endsWith("\"") && identifier.length() > 2) {
            cut = true;
        } else if (identifier.startsWith("[") && identifier.endsWith("]")) {
            cut = true;
        }
        return (cut ? identifier.substring(1, identifier.length() - 1) : identifier).toUpperCase();
    }

    private class PropChains {

        public void addInto(ImmutableProp prop, Map<String, List<ImmutableProp>> map) {
            Storage storage = lazyGetStorage(prop);
            if (prop.isEmbedded(EmbeddedLevel.BOTH)) {
                MultipleJoinColumns multipleJoinColumns = null;
                List<ImmutableProp> baseChain;
                if (prop.isReference(TargetLevel.PERSISTENT)) {
                    if (storage instanceof MultipleJoinColumns) {
                        multipleJoinColumns = (MultipleJoinColumns) storage;
                    }
                    baseChain = new ArrayList<>();
                    baseChain.add(prop);
                    prop = prop.getTargetType().getIdProp();
                } else {
                    baseChain = Collections.emptyList();
                }
                for (Map.Entry<String, EmbeddedColumns.Partial> e : ((EmbeddedColumns)lazyGetStorage(prop)).getPartialMap().entrySet()) {
                    EmbeddedColumns.Partial partial = e.getValue();
                    if (!partial.isEmbedded()) {
                        ImmutableProp partProp = prop;
                        String cmpName = comparableIdentifier(partial.name(0));
                        String path = e.getKey();
                        List<ImmutableProp> chain = new ArrayList<>(baseChain);
                        chain.add(partProp);
                        ImmutableType targetType = partProp.getTargetType();
                        if (path != null) {
                            for (String part : DOT_PATTERN.split(path)) {
                                partProp = targetType.getProp(part);
                                targetType = partProp.getTargetType();
                                chain.add(partProp);
                            }
                        }
                        if (multipleJoinColumns != null) {
                            int index = multipleJoinColumns.size() - 1;
                            while (index >= 0) {
                                String referencedName = multipleJoinColumns.referencedName(index);
                                if (comparableIdentifier(referencedName).equals(cmpName)) {
                                    map.put(multipleJoinColumns.name(index), Collections.unmodifiableList(chain));
                                    break;
                                }
                                --index;
                            }
                            if (index == -1) {
                                throw new AssertionError(
                                        "Internal bug: Cannot find column name by reference columnName"
                                );
                            }
                        } else {
                            map.put(cmpName, Collections.unmodifiableList(chain));
                        }
                    }
                }
            } else if (storage instanceof SingleColumn) {
                String cmpName = comparableIdentifier(((SingleColumn)storage).getName());
                map.put(cmpName, Collections.singletonList(prop));
            }
        }
    }

    public enum AutoForeignKeyPolicy {
        REAL,
        FAKE,
        FORCED_FAKE
    }
}
