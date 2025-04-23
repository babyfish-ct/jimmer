package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.DatabaseValidationIgnore;
import org.babyfish.jimmer.sql.association.meta.AssociationType;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.ast.tuple.Tuple3;
import org.babyfish.jimmer.sql.exception.DatabaseValidationException;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.meta.impl.DatabaseIdentifiers;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DatabaseValidators {

    private final EntityManager entityManager;

    private final String microServiceName;

    private final boolean defaultDissociationActionCheckable;

    private final MetadataStrategy strategy;

    private final Predicate<ImmutableType> predicate;

    private final Connection con;

    private final List<DatabaseValidationException.Item> items;

    private final Map<ImmutableType, org.babyfish.jimmer.lang.Ref<Table>> tableRefMap = new HashMap<>();

    private final Map<ImmutableProp, org.babyfish.jimmer.lang.Ref<Table>> middleTableRefMap = new HashMap<>();

    @Nullable
    public static DatabaseValidationException validate(
            EntityManager entityManager,
            String microServiceName,
            boolean defaultDissociationActionCheckable,
            MetadataStrategy strategy,
            Predicate<ImmutableType> predicate,
            Connection con
    ) throws SQLException {
        return new DatabaseValidators(
                entityManager,
                microServiceName,
                defaultDissociationActionCheckable,
                strategy,
                predicate,
                con
        ).validate();
    }

    private DatabaseValidators(
            EntityManager entityManager,
            String microServiceName,
            boolean defaultDissociationActionCheckable,
            MetadataStrategy strategy,
            Predicate<ImmutableType> predicate,
            Connection con
    ) {
        this.entityManager = entityManager;
        this.microServiceName = microServiceName;
        this.defaultDissociationActionCheckable = defaultDissociationActionCheckable;
        this.strategy = strategy;
        this.con = con;
        this.predicate = predicate != null ?
                predicate :
                type -> !type.getJavaClass().isAnnotationPresent(DatabaseValidationIgnore.class);
        this.items = new ArrayList<>();
    }

    private DatabaseValidationException validate() throws SQLException {
        for (ImmutableType type : entityManager.getAllTypes(microServiceName)) {
            if (type.isEntity() && !(type instanceof AssociationType) && predicate.test(type)) {
                validateSelf(type);
            }
        }
        for (ImmutableType type : entityManager.getAllTypes(microServiceName)) {
            if (type.isEntity() && !(type instanceof AssociationType) && predicate.test(type)) {
                validateForeignKey(type);
            }
        }
        if (!items.isEmpty()) {
            return new DatabaseValidationException(items);
        }
        return null;
    }

    private void validateSelf(ImmutableType type) throws SQLException {
        Table table = tableOf(type);
        if (table == null) {
            return;
        }
        if (!(type instanceof AssociationType) && type.getIdProp().getAnnotation(DatabaseValidationIgnore.class) != null) {
            ColumnDefinition idColumnDefinition = type.getIdProp().getStorage(strategy);
            Set<String> idColumnNames = new LinkedHashSet<>((idColumnDefinition.size() * 4 + 2) / 3);
            for (int i = 0; i < idColumnDefinition.size(); i++) {
                idColumnNames.add(
                        DatabaseIdentifiers.comparableIdentifier(idColumnDefinition.name(i))
                );
            }
            if (!idColumnNames.equals(table.primaryKeyColumns)) {
                items.add(
                        new DatabaseValidationException.Item(
                                type,
                                null,
                                "Expected primary key columns are " +
                                        type.getIdProp().<ColumnDefinition>getStorage(strategy).toColumnNames() +
                                        ", but actual primary key columns are " +
                                        table.primaryKeyColumns
                        )
                );
            }
        }
        for (ImmutableProp prop : type.getProps().values()) {
            if (prop.getAnnotation(DatabaseValidationIgnore.class) != null) {
                continue;
            }
            Storage storage = prop.getStorage(strategy);
            if (storage instanceof ColumnDefinition) {
                ColumnDefinition columnDefinition = (ColumnDefinition)storage;
                for (int i = 0; i < columnDefinition.size(); i++) {
                    Column column = table.columnMap.get(
                            DatabaseIdentifiers.comparableIdentifier(columnDefinition.name(i))
                    );
                    if (column == null) {
                        items.add(
                                new DatabaseValidationException.Item(
                                        type,
                                        prop,
                                        "There is no column \"" +
                                                columnDefinition.name(i) +
                                                "\" in table \"" +
                                                table +
                                                "\""
                                )
                        );
                    }
                }
            }
            if (storage instanceof SingleColumn) {
                Column column = table.columnMap.get(
                        DatabaseIdentifiers.comparableIdentifier(((SingleColumn)storage).getName())
                );
                if (column != null && (!prop.isAssociation(TargetLevel.ENTITY) || prop.isTargetForeignKeyReal(strategy))) {
                    boolean nullable = prop.isNullable() && !prop.isInputNotNull();
                    if (nullable != column.nullable) {
                        items.add(
                                new DatabaseValidationException.Item(
                                        type,
                                        prop,
                                        "The property is " +
                                                (nullable ? "nullable" : "nonnull(include inputNotNull)") +
                                                ", but the database column \"" +
                                                ((SingleColumn) storage).getName() +
                                                "\" in table \"" +
                                                table +
                                                "\" is " +
                                                (column.nullable ? "nullable" : "nonnull")
                                )
                        );
                    }
                }
            }
        }
    }

    private void validateForeignKey(ImmutableType type) throws SQLException {
        Table table = tableOf(type);
        if (table == null) {
            return;
        }
        for (ImmutableProp prop : type.getProps().values()) {
            if (!prop.isAssociation(TargetLevel.PERSISTENT) ||
                    prop.getAnnotation(DatabaseValidationIgnore.class) != null ||
                    predicate.test(prop.getTargetType())) {
                continue;
            }
            ForeignKeyContext ctx = new ForeignKeyContext(this, type, prop);
            Storage storage = prop.getStorage(strategy);
            if (storage instanceof MiddleTable) {
                Table middleTable = middleTableOf(prop);
                if (middleTable != null) {
                    MiddleTable middleTableMeta = (MiddleTable) storage;
                    if (middleTableMeta.getColumnDefinition().isForeignKey()) {
                        ForeignKey thisForeignKey = middleTable.getForeignKey(
                                ctx,
                                middleTableMeta.getColumnDefinition(),
                                defaultDissociationActionCheckable
                        );
                        if (thisForeignKey != null) {
                            thisForeignKey.assertReferencedColumns(ctx, type);
                        }
                    }
                    if (middleTableMeta.getTargetColumnDefinition().isForeignKey()) {
                        ForeignKey targetForeignKey = middleTable.getForeignKey(
                                ctx,
                                middleTableMeta.getTargetColumnDefinition(),
                                defaultDissociationActionCheckable
                        );
                        if (targetForeignKey != null) {
                            targetForeignKey.assertReferencedColumns(ctx, prop.getTargetType());
                        }
                    }
                    assertMiddleTablePrimaryKey(prop, middleTableMeta, middleTable);
                }
            } else if (storage != null && prop.isReference(TargetLevel.PERSISTENT)) {
                ColumnDefinition columnDefinition = prop.getStorage(strategy);
                if (columnDefinition.isForeignKey()) {
                    ForeignKey foreignKey = table.getForeignKey(
                            ctx,
                            columnDefinition,
                            defaultDissociationActionCheckable
                    );
                    if (foreignKey != null) {
                        foreignKey.assertReferencedColumns(ctx, prop.getTargetType());
                    }
                }
            }
        }
    }

    private Table tableOf(ImmutableType type) throws SQLException {
        org.babyfish.jimmer.lang.Ref<Table> tableRef = tableRefMap.get(type);
        if (tableRef == null) {
            Set<Table> tables = tablesOf(DatabaseIdentifiers.rawIdentifier(type.getTableName(strategy)));
            if (tables.isEmpty()) {
                items.add(
                        new DatabaseValidationException.Item(
                                type,
                                null,
                                "There is no table \"" +
                                        DatabaseIdentifiers.rawIdentifier(type.getTableName(strategy)) +
                                        "\""
                        )
                );
                tableRef = Ref.empty();
            } else if (tables.size() > 1) {
                items.add(
                        new DatabaseValidationException.Item(
                                type,
                                null,
                                "Too many matched tables: " + tables +
                                        ", please try configure `setDatabaseValidationCatalog` " +
                                        "or `setDatabaseValidationSchema`"
                        )
                );
                tableRef = Ref.empty();
            } else {
                Table table = tables.iterator().next();
                table = new Table(table, columnsOf(table), primaryKeyColumns(table));
                tableRef = Ref.of(table);
            }
            tableRefMap.put(type, tableRef);
        }
        return tableRef.getValue();
    }

    private Table middleTableOf(ImmutableProp prop) throws SQLException {
        Ref<Table> tableRef = middleTableRefMap.get(prop);
        if (tableRef == null) {
            Storage storage = prop.getStorage(strategy);
            if (storage instanceof MiddleTable) {
                MiddleTable middleTable = (MiddleTable) storage;
                Set<Table> tables = tablesOf(middleTable.getTableName());
                if (tables.isEmpty()) {
                    items.add(
                            new DatabaseValidationException.Item(
                                    prop.getDeclaringType(),
                                    prop,
                                    "There is no table \"" +
                                            middleTable.getTableName() +
                                            "\""
                            )
                    );
                    tableRef = Ref.empty();
                } else if (tables.size() > 1) {
                    items.add(
                            new DatabaseValidationException.Item(
                                    prop.getDeclaringType(),
                                    prop,
                                    "Too many matched tables: " + tables
                            )
                    );
                    tableRef = Ref.empty();
                } else {
                    Table table = tables.iterator().next();
                    table = new Table(
                            table,
                            columnsOf(table),
                            primaryKeyColumns(table)
                    );
                    tableRef = Ref.of(table);
                }
            } else {
                tableRef = Ref.empty();
            }
            middleTableRefMap.put(prop, tableRef);
        }
        return tableRef.getValue();
    }

    private Set<Table> tablesOf(String table) throws SQLException {
        String catalogName = null;
        String schemaName = null;
        String tableName = table;
        int index = tableName.lastIndexOf('.');
        if (index != -1) {
            schemaName = tableName.substring(0, index);
            tableName = tableName.substring(index + 1);
            index = schemaName.lastIndexOf('.');
            if (index != -1) {
                schemaName = schemaName.substring(index + 1);
                catalogName = schemaName.substring(0, index);
            }
        }
        return tablesOf(
                required(catalogName, con.getCatalog()),
                required(schemaName, con.getSchema()),
                tableName
        );
    }

    private static String required(String value, String defaultValue) {
        if (value == null || value.isEmpty() || "null".equals(value)) {
            return defaultValue;
        }
        return value;
    }

    private Set<Table> tablesOf(String catalogName, String schemaName, String tableName) throws SQLException {
        Set<Tuple3<String, String, String>> tuples = new LinkedHashSet<>();
        new TableNameCollector(
                new String[] { catalogName, schemaName, tableName },
                arr -> tuples.add(new Tuple3<>(arr[0], arr[1], arr[2]))
        ).emit();
        for (Tuple3<String, String, String> tuple : tuples) {
            Set<Table> tables = tablesOf0(tuple.get_1(), tuple.get_2(), tuple.get_3());
            if (!tables.isEmpty()) {
                return tables;
            }
        }
        return Collections.emptySet();
    }

    private Set<Table> tablesOf0(String catalogName, String schemaName, String tableName) throws SQLException {
        Set<Table> tables = new LinkedHashSet<>();
        try (ResultSet rs = con.getMetaData().getTables(
                catalogName,
                schemaName,
                tableName,
                null
        )) {
            while (rs.next()) {
                tables.add(
                        new Table(
                                rs.getString("TABLE_CAT"),
                                rs.getString("TABLE_SCHEM"),
                                rs.getString("TABLE_NAME")
                        )
                );
            }
        }
        return tables
                .stream()
                .filter(it -> it.name.equalsIgnoreCase(tableName))
                .collect(Collectors.toSet());
    }

    private Map<String, Column> columnsOf(Table table) throws SQLException {
        Map<String, Column> columnMap = new HashMap<>();
        try (ResultSet rs = con.getMetaData().getColumns(
                table.catalog,
                table.schema,
                table.name,
                null
        )) {
            while (rs.next()) {
                Column column = new Column(
                        table,
                        rs.getString("COLUMN_NAME").toUpperCase(),
                        rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable
                );
                columnMap.put(column.name.toUpperCase(), column);
            }
        }
        return columnMap;
    }

    private Set<String> primaryKeyColumns(Table table) throws SQLException {
        Set<String> columnNames = new HashSet<>();
        try (ResultSet rs = con.getMetaData().getPrimaryKeys(
                table.catalog,
                table.schema,
                table.name
        )) {
            while (rs.next()) {
                columnNames.add(
                        rs.getString("COLUMN_NAME").toUpperCase()
                );
            }
        }
        return columnNames;
    }

    private Map<Set<String>, ForeignKey> foreignKeys(Table table) throws SQLException {
        Map<Tuple2<String, Table>, Map<String, String>> map = new HashMap<>();
        try (ResultSet rs = con.getMetaData().getImportedKeys(
                table.catalog,
                table.schema,
                table.name
        )) {
            while (rs.next()) {
                String constraintName = rs.getString("FK_NAME").toUpperCase();
                Table referencedTable = tablesOf(
                        upper(rs.getString("PKTABLE_CAT")),
                        upper(rs.getString("PKTABLE_SCHEM")),
                        rs.getString("PKTABLE_NAME").toUpperCase()
                ).iterator().next();
                String columnName = upper(rs.getString("FKCOLUMN_NAME"));
                String referencedColumnName = upper(rs.getString("PKCOLUMN_NAME"));
                map.computeIfAbsent(
                        new Tuple2<>(constraintName, referencedTable),
                        it -> new LinkedHashMap<>()
                ).put(columnName, referencedColumnName);
            }
        }
        if (map.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Set<String>, ForeignKey> foreignKeyMap = new HashMap<>();
        for (Map.Entry<Tuple2<String, Table>, Map<String, String>> e : map.entrySet()) {
            String constraintName = e.getKey().get_1();
            Table referencedTable = e.getKey().get_2();
            Map<String, String> subMap = e.getValue();
            Set<String> columnNames = subMap.keySet();
            Collection<String> referencedColumnNames = subMap.values();
            ForeignKey foreignKey = new ForeignKey(
                    constraintName,
                    columnNames,
                    referencedTable,
                    new LinkedHashSet<>(referencedColumnNames)
            );
            foreignKeyMap.put(
                    columnNames,
                    foreignKey
            );
        }
        return foreignKeyMap;
    }

    private void assertMiddleTablePrimaryKey(ImmutableProp prop, MiddleTable meta, Table table) {

        Set<String> unmanagedColumnNames = new LinkedHashSet<>();

        ColumnDefinition cd = meta.getColumnDefinition();
        for (int i = cd.size() - 1; i >= 0; --i) {
            if (!table.primaryKeyColumns.contains(DatabaseIdentifiers.comparableIdentifier(cd.name(i)))) {
                unmanagedColumnNames.add(cd.name(i));
            }
        }

        ColumnDefinition tcd = meta.getTargetColumnDefinition();
        for (int i = tcd.size() - 1; i >= 0; --i) {
            if (!table.primaryKeyColumns.contains(DatabaseIdentifiers.comparableIdentifier(tcd.name(i)))) {
                unmanagedColumnNames.add(tcd.name(i));
            }
        }

        LogicalDeletedInfo ldi = meta.getLogicalDeletedInfo();
        if (ldi != null) {
            if (!table.primaryKeyColumns.contains(DatabaseIdentifiers.comparableIdentifier(ldi.getColumnName()))) {
                unmanagedColumnNames.add(ldi.getColumnName());
            }
        }

        JoinTableFilterInfo fi = meta.getFilterInfo();
        if (fi != null) {
            if (!table.primaryKeyColumns.contains(DatabaseIdentifiers.comparableIdentifier(fi.getColumnName()))) {
                unmanagedColumnNames.add(fi.getColumnName());
            }
        }

        for (String unmanagedColumnName : unmanagedColumnNames) {
            items.add(
                    new DatabaseValidationException.Item(
                            prop.getDeclaringType(),
                            prop,
                            "The primary key of middle table must contain all columns, " +
                                    "but column \"" +
                                    unmanagedColumnName +
                                    "\" of table \"" +
                                    meta.getTableName() +
                                    "\" is not part of the primary key"
                    )
            );
        }
    }

    private static String upper(String text) {
        return text == null ? null : text.toUpperCase();
    }

    private static class Table {

        // keep the same case
        final String catalog;

        // keep the same case
        final String schema;

        // keep the same case
        final String name;

        final Map<String, Column> columnMap;

        final Set<String> primaryKeyColumns;

        private Map<Set<String>, ForeignKey> _foreignKeyMap;

        Table(String catalog, String schema, String name) {
            this.catalog = catalog;
            this.schema = schema;
            this.name = name;
            this.columnMap = Collections.emptyMap();
            this.primaryKeyColumns = Collections.emptySet();
        }

        public Table(
                Table base,
                Map<String, Column> columnMap,
                Set<String> primaryKeyColumns
        ) {
            this.catalog = base.catalog;
            this.schema = base.schema;
            this.name = base.name;
            this.columnMap = columnMap;
            this.primaryKeyColumns = primaryKeyColumns;
        }

        public ForeignKey getForeignKey(
                ForeignKeyContext ctx,
                ColumnDefinition columnDefinition,
                boolean defaultDissociationActionCheckable
        ) throws SQLException{
            ForeignKey foreignKey;
            if (columnDefinition instanceof MultipleJoinColumns) {
                MultipleJoinColumns multipleJoinColumns = (MultipleJoinColumns) columnDefinition;
                Set<String> columnNames = new LinkedHashSet<>();
                for (int i = 0; i < multipleJoinColumns.size(); i++) {
                    columnNames.add(multipleJoinColumns.name(i).toUpperCase());
                }
                foreignKey = getForeignKeyMap(ctx).get(columnNames);
            } else {
                foreignKey = getForeignKeyMap(ctx).get(
                        Collections.singleton(
                                ((SingleColumn) columnDefinition).getName().toUpperCase()
                        )
                );
            }
            if (columnDefinition.isForeignKey() && foreignKey == null) {
                ctx.databaseValidators.items.add(
                        new DatabaseValidationException.Item(
                                ctx.type,
                                ctx.prop,
                                "No foreign key constraint for columns: " + columnDefinition.toColumnNames() +
                                        ". If this column(s) is(are) a real foreign key, " +
                                        "please add foreign key constraint in database" +
                                        "; If this column is a fake foreign key, " +
                                        "please use `@JoinColumn(foreignKey = false, ...)`"
                        )
                );
            }
            if (!defaultDissociationActionCheckable && !columnDefinition.isForeignKey() && foreignKey != null) {
                ctx.databaseValidators.items.add(
                        new DatabaseValidationException.Item(
                                ctx.type,
                                ctx.prop,
                                "Unnecessary foreign key constraint for columns: " + columnDefinition.toColumnNames() +
                                        ". If this column(s) is(are) a fake foreign key, " +
                                        "please remove foreign key constraint in database" +
                                        "; If this column is a real foreign key, " +
                                        "please use `@JoinColumn(foreignKey = true, ...)`"
                        )
                );
            }
            return foreignKey;
        }

        @Override
        public int hashCode() {
            return Objects.hash(catalog, schema, name);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Table table = (Table) o;
            return catalog.equals(table.catalog) &&
                    schema.equals(table.schema) &&
                    name.equals(table.name);
        }

        @Override
        public String toString() {
            return catalog + '.' + schema + '.' + name;
        }

        private Map<Set<String>, ForeignKey> getForeignKeyMap(ForeignKeyContext ctx) throws SQLException {
            Map<Set<String>, ForeignKey> map = _foreignKeyMap;
            if (map == null) {
                map = ctx.databaseValidators.foreignKeys(this);
                _foreignKeyMap = map;
            }
            return map;
        }
    }

    private static class Column {

        final Table table;

        // Always capitalized
        final String name;

        final boolean nullable;

        private Column(Table table, String name, boolean nullable) {
            this.table = table;
            this.name = name.toUpperCase();
            this.nullable = nullable;
        }
    }

    private static class ForeignKey {

        // Always capitalized
        final String constraintName;

        // Always capitalized
        final Set<String> columnNames;

        final Table referencedTable;

        // Always capitalized
        final Set<String> referenceColumNames;

        ForeignKey(
                String constraintName,
                Set<String> columnNames,
                Table referencedTable,
                Set<String> referenceColumNames
        ) {
            this.constraintName = constraintName;
            this.columnNames = columnNames;
            this.referencedTable = referencedTable;
            this.referenceColumNames = referenceColumNames;
        }

        void assertReferencedColumns(
                ForeignKeyContext ctx,
                ImmutableType referencedType
        ) {
            if (!referencedType
                    .getIdProp()
                    .<ColumnDefinition>getStorage(ctx.databaseValidators.strategy)
                    .toColumnNames()
                    .equals(referenceColumNames)) {
                ctx.databaseValidators.items.add(
                        new DatabaseValidationException.Item(
                                ctx.type,
                                ctx.prop,
                                "Illegal foreign key \"" +
                                        constraintName +
                                        "\", referenced column(s) is " +
                                        referenceColumNames +
                                        ", but the column(s) of \"" +
                                        referencedType.getIdProp() +
                                        "\" is " +
                                        referencedType
                                                .getIdProp()
                                                .<ColumnDefinition>getStorage(ctx.databaseValidators.strategy)
                                                .toColumnNames()
                        )
                );
            }
        }
    }

    private static class ForeignKeyContext {

        final DatabaseValidators databaseValidators;

        final ImmutableType type;

        final ImmutableProp prop;

        private ForeignKeyContext(DatabaseValidators databaseValidators, ImmutableType type, ImmutableProp prop) {
            this.databaseValidators = databaseValidators;
            this.type = type;
            this.prop = prop;
        }
    }

    private class TableNameCollector {

        private final String[] originalNames;

        private final String[] currentNames;

        private final Consumer<String[]> emitter;

        private TableNameCollector(String[] originalNames, Consumer<String[]> emitter) {
            this.originalNames = originalNames;
            this.currentNames = new String[originalNames.length];
            this.emitter = emitter;
        }

        public void emit() {
            emit(0);
        }

        private void emit(int depth) {

            String text = originalNames[depth];
            currentNames[depth] = text;
            if (depth + 1 < originalNames.length) {
                emit(depth + 1);
            } else {
                emitter.accept(currentNames);
            }

            if (text != null && !text.isEmpty()) {
                currentNames[depth] = text.toUpperCase();
                if (depth + 1 < originalNames.length) {
                    emit(depth + 1);
                } else {
                    emitter.accept(currentNames);
                }

                currentNames[depth] = text.toLowerCase();
                if (depth + 1 < originalNames.length) {
                    emit(depth + 1);
                } else {
                    emitter.accept(currentNames);
                }
            }
        }
    }
}
