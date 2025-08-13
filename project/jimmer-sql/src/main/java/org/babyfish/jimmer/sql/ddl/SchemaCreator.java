package org.babyfish.jimmer.sql.ddl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.GeneratedValue;
import org.babyfish.jimmer.sql.ManyToMany;
import org.babyfish.jimmer.sql.ddl.annotations.ColumnDef;
import org.babyfish.jimmer.sql.ddl.annotations.Kind;
import org.babyfish.jimmer.sql.ddl.annotations.OnDeleteAction;
import org.babyfish.jimmer.sql.ddl.annotations.Unique;
import org.babyfish.jimmer.sql.ddl.fake.FakeImmutablePropImpl;
import org.babyfish.jimmer.sql.ddl.fake.FakeImmutableTypeImpl;
import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.babyfish.jimmer.sql.meta.Storage;
import org.babyfish.jimmer.sql.meta.impl.Storages;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.sql.DatabaseMetaData;
import java.util.*;

/**
 * @author honhimW
 */

public class SchemaCreator implements Exporter<Collection<ImmutableType>> {

    private static final Logger log = LoggerFactory.getLogger("jimmer.ddl.sql");

    private final JSqlClientImplementor client;

    private DatabaseVersion version;

    private StandardTableExporter standardTableExporter;

    private StandardForeignKeyExporter standardForeignKeyExporter;

    private StandardSequenceExporter standardSequenceExporter;

    public SchemaCreator(JSqlClientImplementor client) {
        this(client, null);
    }

    public SchemaCreator(JSqlClientImplementor client, DatabaseVersion version) {
        this.client = client;
        this.version = version;
    }

    /**
     * do get database meta-data
     */
    public void init() {
        if (version == null) {
            version = client.getConnectionManager().execute(connection -> {
                try {
                    DatabaseMetaData metaData = connection.getMetaData();
                    int databaseMajorVersion = metaData.getDatabaseMajorVersion();
                    int databaseMinorVersion = metaData.getDatabaseMinorVersion();
                    String databaseProductVersion = metaData.getDatabaseProductVersion();
                    return new DatabaseVersion(databaseMajorVersion, databaseMinorVersion, databaseProductVersion);
                } catch (Exception e) {
                    // cannot get database version, using latest as default
                    return DatabaseVersion.LATEST;
                }
            });
        }
        standardTableExporter = new StandardTableExporter(client, version);
        standardForeignKeyExporter = new StandardForeignKeyExporter(client, version);
        standardSequenceExporter = new StandardSequenceExporter(client, version);
    }

    @Override
    public List<String> getSqlCreateStrings(Collection<ImmutableType> exportable) {
        final List<String> allSqlCreateStrings = new ArrayList<>();
        applyCreateSequences(exportable, allSqlCreateStrings);
        // Middle Table
        exportable = applyConstructMiddleTables(exportable, true);
        applyCreateTables(exportable, allSqlCreateStrings);
        applyCreateForeignKeys(exportable, allSqlCreateStrings);
        return allSqlCreateStrings;
    }

    @Override
    public List<String> getSqlDropStrings(Collection<ImmutableType> exportable) {
        final List<String> allSqlCreateStrings = new ArrayList<>();
        // Middle Table
        exportable = applyConstructMiddleTables(exportable, false);
        applyDropForeignKeys(exportable, allSqlCreateStrings);
        applyDropTables(exportable, allSqlCreateStrings);
        applyDropSequences(exportable, allSqlCreateStrings);
        return allSqlCreateStrings;
    }

    private void applyCreateSequences(Collection<ImmutableType> exportable, List<String> allSqlCreateStrings) {
        if (log.isDebugEnabled()) {
            log.debug("1. start create sequences.");
        }
        int i = 1;
        for (ImmutableType immutableType : exportable) {
            Map<String, ImmutableProp> allDefinitionProps = DDLUtils.allDefinitionProps(immutableType);
            for (Map.Entry<String, ImmutableProp> entry : allDefinitionProps.entrySet()) {
                ImmutableProp definitionProp = entry.getValue();
                if (definitionProp.getAnnotation(GeneratedValue.class) != null) {
                    List<String> sqlCreateStrings = standardSequenceExporter.getSqlCreateStrings(definitionProp);
                    allSqlCreateStrings.addAll(sqlCreateStrings);
                    if (log.isDebugEnabled()) {
                        for (String sqlCreateString : sqlCreateStrings) {
                            log.debug("1.{}. {}", i++, sqlCreateString);
                        }
                    }
                }
            }
        }
    }

    private void applyCreateTables(Collection<ImmutableType> exportable, List<String> allSqlCreateStrings) {
        if (log.isDebugEnabled()) {
            log.debug("2. start create tables.");
        }

        int i = 1;
        for (ImmutableType immutableType : exportable) {
            List<String> sqlCreateStrings = standardTableExporter.getSqlCreateStrings(immutableType);
            allSqlCreateStrings.addAll(sqlCreateStrings);
            if (log.isDebugEnabled()) {
                for (String sqlCreateString : sqlCreateStrings) {
                    log.debug("2.{}. {}", i++, sqlCreateString);
                }
            }
        }
    }

    private Collection<ImmutableType> applyConstructMiddleTables(Collection<ImmutableType> exportable, boolean isCreate) {
        Set<String> tableNames = new HashSet<>();
        for (ImmutableType immutableType : exportable) {
            tableNames.add(immutableType.getTableName(client.getMetadataStrategy()));
        }

        List<ImmutableType> withMiddleTables = new ArrayList<>(exportable);
        for (ImmutableType immutableType : exportable) {
            for (ImmutableProp prop : immutableType.getProps().values()) {
                if (prop.isReferenceList(TargetLevel.PERSISTENT)) {
                    ManyToMany manyToMany = prop.getAnnotation(ManyToMany.class);
                    if (manyToMany != null) {
                        Storage storage = Storages.of(prop, client.getMetadataStrategy());
                        if (storage instanceof MiddleTable) {
                            MiddleTable middleTable = (MiddleTable) storage;
                            String middleTableName = middleTable.getTableName();
                            if (tableNames.contains(middleTableName)) {
                                if (log.isDebugEnabled()) {
                                    log.debug("Entity with table name `{}` already defines the intermediate table, ignoring middle-table.", middleTableName);
                                }
                                continue;
                            }

                            ImmutableProp joinProp = immutableType.getIdProp();
                            ImmutableProp inverseJoinProp = prop.getTargetType().getIdProp();
                            String joinColumnName = client.getMetadataStrategy().getNamingStrategy().middleTableBackRefColumnName(prop);
                            String inverseJoinColumnName = client.getMetadataStrategy().getNamingStrategy().middleTableTargetRefColumnName(prop);
                            org.babyfish.jimmer.sql.ddl.annotations.MiddleTable annotation = prop.getAnnotation(org.babyfish.jimmer.sql.ddl.annotations.MiddleTable.class);
                            boolean useAutoId;
                            boolean useRealForeignKey;
                            String comment;
                            String tableType;
                            org.babyfish.jimmer.sql.ddl.annotations.ForeignKey joinColumnForeignKey;
                            org.babyfish.jimmer.sql.ddl.annotations.ForeignKey inverseJoinColumnForeignKey;
                            if (annotation != null) {
                                comment = annotation.comment();
                                tableType = annotation.tableType();
                                useAutoId = annotation.useAutoId();
                                useRealForeignKey = annotation.useRealForeignKey();
                                joinColumnForeignKey = annotation.joinColumnForeignKey();
                                inverseJoinColumnForeignKey = annotation.inverseJoinColumnForeignKey();
                            } else {
                                useAutoId = false;
                                useRealForeignKey = true;
                                comment = "";
                                tableType = "";
                                joinColumnForeignKey = new DDLUtils.DefaultForeignKey();
                                inverseJoinColumnForeignKey = new DDLUtils.DefaultForeignKey();
                            }

                            FakeImmutableTypeImpl fakeImmutableType = new FakeImmutableTypeImpl();
                            fakeImmutableType.tableName = middleTableName;
                            fakeImmutableType.javaClass = Object.class;
                            fakeImmutableType.props = new LinkedHashMap<>();

                            FakeImmutablePropImpl id = new FakeImmutablePropImpl();
                            id.name = "id";
                            Map<String, ImmutableProp> props;
                            if (useAutoId) {
                                id.returnClass = Integer.TYPE;
                                id.isId = true;
                                id.isColumnDefinition = true;
                                id.annotations = new Annotation[]{new DDLUtils.DefaultGeneratedValue()};
                                fakeImmutableType.props.put(id.name, id);
                                fakeImmutableType.idProp = id;
                                props = fakeImmutableType.props;
                            } else {
                                id.returnClass = Object.class;
                                id.isId = true;
                                id.isColumnDefinition = false;
                                id.annotations = new Annotation[]{new DDLUtils.DefaultGeneratedValue()};
                                id.isEmbedded = true;
                                FakeImmutableTypeImpl embeddedIdType = new FakeImmutableTypeImpl();
                                id.targetType = embeddedIdType;
                                embeddedIdType.props = new LinkedHashMap<>();
                                embeddedIdType.selectableProps = embeddedIdType.props;
                                props = embeddedIdType.props;
                                fakeImmutableType.props.put(id.name, id);
                                fakeImmutableType.idProp = id;
                            }

                            FakeImmutablePropImpl fakeJoinProp = new FakeImmutablePropImpl();
                            fakeJoinProp.name = joinColumnName;
                            fakeJoinProp.returnClass = joinProp.getReturnClass();
                            fakeJoinProp.isColumnDefinition = true;
                            if (useRealForeignKey) {
                                fakeJoinProp.annotations = new Annotation[]{new DDLUtils.DefaultColumnDef() {
                                    @Override
                                    public org.babyfish.jimmer.sql.ddl.annotations.ForeignKey foreignKey() {
                                        return joinColumnForeignKey;
                                    }
                                }};
                                fakeJoinProp.isTargetForeignKeyReal = true;
                                fakeJoinProp.targetType = immutableType;
                            }
                            props.put(fakeJoinProp.name, fakeJoinProp);

                            FakeImmutablePropImpl fakeInverseJoin = new FakeImmutablePropImpl();
                            fakeInverseJoin.name = inverseJoinColumnName;
                            fakeInverseJoin.returnClass = inverseJoinProp.getReturnClass();
                            fakeInverseJoin.isColumnDefinition = true;
                            if (useRealForeignKey) {
                                fakeInverseJoin.annotations = new Annotation[]{new DDLUtils.DefaultColumnDef() {
                                    @Override
                                    public org.babyfish.jimmer.sql.ddl.annotations.ForeignKey foreignKey() {
                                        return inverseJoinColumnForeignKey;
                                    }
                                }};
                                fakeInverseJoin.isTargetForeignKeyReal = true;
                                fakeInverseJoin.targetType = prop.getTargetType();
                            }
                            props.put(fakeInverseJoin.name, fakeInverseJoin);

                            fakeImmutableType.tableDef = new DDLUtils.DefaultTableDef() {
                                @Override
                                public String tableType() {
                                    return tableType;
                                }

                                @Override
                                public Unique[] uniques() {
                                    return new Unique[]{
                                        new DDLUtils.DefaultUnique() {
                                            @Override
                                            public String[] columns() {
                                                if (useAutoId) {
                                                    return new String[]{joinColumnName, inverseJoinColumnName};
                                                } else {
                                                    return new String[0];
                                                }
                                            }

                                            @Override
                                            public Kind kind() {
                                                return Kind.NAME;
                                            }
                                        }
                                    };
                                }

                                @Override
                                public String comment() {
                                    return comment;
                                }
                            };
                            fakeImmutableType.selectableProps = fakeImmutableType.props;
                            if (isCreate) {
                                withMiddleTables.add(fakeImmutableType);
                            } else {
                                withMiddleTables.add(0, fakeImmutableType);
                            }
                        }
                    }
                }
            }
        }
        return withMiddleTables;
    }

    private void applyCreateForeignKeys(Collection<ImmutableType> exportable, List<String> allSqlCreateStrings) {
        if (!client.getDialect().isForeignKeySupported()) {
            if (log.isDebugEnabled()) {
                log.debug("3. `{}` does not supports foreign key.", client.getDialect().getClass().getSimpleName());
            }
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("3. start create foreign keys.");
        }
        int i = 1;
        for (ImmutableType immutableType : exportable) {
            for (ForeignKey foreignKey : DDLUtils.getForeignKeys(client.getMetadataStrategy(), immutableType)) {
                List<String> sqlCreateStrings = standardForeignKeyExporter.getSqlCreateStrings(foreignKey);
                allSqlCreateStrings.addAll(sqlCreateStrings);
                if (log.isDebugEnabled()) {
                    for (String sqlCreateString : sqlCreateStrings) {
                        log.debug("3.{}. {}", i++, sqlCreateString);
                    }
                }
            }
        }
    }

    private void applyDropForeignKeys(Collection<ImmutableType> exportable, List<String> allSqlCreateStrings) {
        if (!client.getDialect().isForeignKeySupported()) {
            if (log.isDebugEnabled()) {
                log.debug("1. `{}` does not supports foreign key.", client.getDialect().getClass().getSimpleName());
            }
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("1. start drop foreign keys.");
        }
        int i = 1;
        for (ImmutableType immutableType : exportable) {
            for (ForeignKey foreignKey : DDLUtils.getForeignKeys(client.getMetadataStrategy(), immutableType)) {
                List<String> sqlCreateStrings = standardForeignKeyExporter.getSqlDropStrings(foreignKey);
                allSqlCreateStrings.addAll(sqlCreateStrings);
                if (log.isDebugEnabled()) {
                    for (String sqlCreateString : sqlCreateStrings) {
                        log.debug("1.{}. {}", i++, sqlCreateString);
                    }
                }
            }
        }
    }

    private void applyDropTables(Collection<ImmutableType> exportable, List<String> allSqlCreateStrings) {
        if (log.isDebugEnabled()) {
            log.debug("2. start drop tables.");
        }
        int i = 1;
        for (ImmutableType immutableType : exportable) {
            List<String> sqlDropStrings = standardTableExporter.getSqlDropStrings(immutableType);
            allSqlCreateStrings.addAll(sqlDropStrings);
            if (log.isDebugEnabled()) {
                for (String sqlDropString : sqlDropStrings) {
                    log.debug("2.{}. {}", i++, sqlDropString);
                }
            }
        }
    }

    private void applyDropSequences(Collection<ImmutableType> exportable, List<String> allSqlCreateStrings) {
        if (log.isDebugEnabled()) {
            log.debug("3. start drop sequences.");
        }
        int i = 1;
        for (ImmutableType immutableType : exportable) {
            Map<String, ImmutableProp> allDefinitionProps = DDLUtils.allDefinitionProps(immutableType);
            for (Map.Entry<String, ImmutableProp> entry : allDefinitionProps.entrySet()) {
                ImmutableProp definitionProp = entry.getValue();
                if (definitionProp.getAnnotation(GeneratedValue.class) != null) {
                    List<String> sqlCreateStrings = standardSequenceExporter.getSqlDropStrings(definitionProp);
                    allSqlCreateStrings.addAll(sqlCreateStrings);
                    if (log.isDebugEnabled()) {
                        for (String sqlCreateString : sqlCreateStrings) {
                            log.debug("3.{}. {}", i++, sqlCreateString);
                        }
                    }
                }
            }
        }
    }

}
