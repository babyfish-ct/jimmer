package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.impl.DatabaseIdentifiers;
import org.babyfish.jimmer.sql.DatabaseValidationIgnore;
import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.meta.*;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.*;

public class DatabaseValidators {

    private final Connection con;

    private final List<DatabaseValidationException.Item> items;

    private Map<ImmutableType, org.babyfish.jimmer.lang.Ref<Table>> tableRefMap = new HashMap<>();

    private Map<ImmutableProp, org.babyfish.jimmer.lang.Ref<Table>> middleTableRefMap = new HashMap<>();

    @Nullable
    public static DatabaseValidationException validate(EntityManager entityManager, Connection con) throws SQLException {
        return new DatabaseValidators(con).validate(entityManager);
    }

    private DatabaseValidators(Connection con) {
        this.con = con;
        this.items = new ArrayList<>();
    }

    private DatabaseValidationException validate(EntityManager entityManager) throws SQLException {
        for (ImmutableType type : entityManager.getAllTypes()) {
            if (type.isEntity() && !type.getJavaClass().isAnnotationPresent(DatabaseValidationIgnore.class)) {
                validateSelf(type);
            }
        }
        for (ImmutableType type : entityManager.getAllTypes()) {
            if (type.isEntity() && !type.getJavaClass().isAnnotationPresent(DatabaseValidationIgnore.class)) {
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
        if (type.getIdProp().getAnnotation(DatabaseValidationIgnore.class) != null &&
                !type.getIdProp().<ColumnDefinition>getStorage().toColumnNames().equals(table.primaryKeyColumns)) {
            items.add(
                    new DatabaseValidationException.Item(
                            type,
                            null,
                            "Expected primary key columns are " +
                                    type.getIdProp().<ColumnDefinition>getStorage().toColumnNames() +
                                    ", but actual primary key columns are " +
                                    table.primaryKeyColumns
                    )
            );
        }
        for (ImmutableProp prop : type.getProps().values()) {
            if (prop.getAnnotation(DatabaseValidationIgnore.class) != null) {
                continue;
            }
            Storage storage = prop.getStorage();
            if (storage instanceof ColumnDefinition) {
                ColumnDefinition columnDefinition = (ColumnDefinition)storage;
                for (int i = 0; i < columnDefinition.size(); i++) {
                    Column column = table.columnMap.get(DatabaseIdentifiers.comparableIdentifier(columnDefinition.name(i)));
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
                Column column = table.columnMap.get(DatabaseIdentifiers.comparableIdentifier(((SingleColumn)storage).getName()));
                if (column != null) {
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
        for (ImmutableProp prop : type.getProps().values()) {
            if (!prop.isAssociation(TargetLevel.PERSISTENT) ||
                    prop.getAnnotation(DatabaseValidationIgnore.class) != null ||
                    prop.getTargetType().getJavaClass().isAnnotationPresent(DatabaseValidationIgnore.class)) {
                continue;
            }
            ForeignKeyContext ctx = new ForeignKeyContext(this, type, prop);
            Storage storage = prop.getStorage();
            if (storage instanceof MiddleTable) {
                Table middleTable = middleTableOf(prop);
                MiddleTable middleTableMeta = (MiddleTable) storage;
                if (middleTableMeta.getColumnDefinition().isForeignKey()) {
                    ForeignKey thisForeignKey = middleTable.getForeignKey(ctx, middleTableMeta.getColumnDefinition());
                    if (thisForeignKey != null) {
                        thisForeignKey.assertReferencedColumns(ctx, type);
                    }
                }
                if (middleTableMeta.getTargetColumnDefinition().isForeignKey()) {
                    ForeignKey targetForeignKey = middleTable.getForeignKey(ctx, middleTableMeta.getTargetColumnDefinition());
                    if (targetForeignKey != null) {
                        targetForeignKey.assertReferencedColumns(ctx, prop.getTargetType());
                    }
                }
            } else if (storage != null && prop.isReference(TargetLevel.PERSISTENT)) {
                ColumnDefinition columnDefinition = prop.getStorage();
                if (columnDefinition.isForeignKey()) {
                    ForeignKey foreignKey = table.getForeignKey(ctx, columnDefinition);
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
            Set<Table> tables = tablesOf(type.getTableName());
            if (tables.isEmpty()) {
                items.add(
                        new DatabaseValidationException.Item(
                                type,
                                null,
                                "There is no table \"" +
                                        type.getTableName() +
                                        "\""
                        )
                );
                tableRef = Ref.empty();
            } else if (tables.size() > 1) {
                items.add(
                        new DatabaseValidationException.Item(
                                type,
                                null,
                                "Too many matched tables: " + tables
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
            Storage storage = prop.getStorage();
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
        String tableName = DatabaseIdentifiers.comparableIdentifier(table);
        int index = tableName.lastIndexOf('.');
        if (index != -1) {
            schemaName = tableName.substring(0, index);
            tableName = tableName.substring(index + 1);
            index = schemaName.lastIndexOf('.');
            if (index != -1) {
                catalogName = schemaName.substring(0, index);
                schemaName = schemaName.substring(index + 1);
            }
        }
        return tablesOf(catalogName, schemaName, tableName);
    }

    private Set<Table> tablesOf(String catalogName, String schemaName, String tableName) throws SQLException {
        Set<Table> tables = new LinkedHashSet<>();
        try (ResultSet rs = con.getMetaData().getTables(
                DatabaseIdentifiers.comparableIdentifier(catalogName),
                DatabaseIdentifiers.comparableIdentifier(schemaName),
                DatabaseIdentifiers.comparableIdentifier(tableName),
                null
        )) {
            while (rs.next()) {
                tables.add(
                        new Table(
                                DatabaseIdentifiers.comparableIdentifier(rs.getString("TABLE_CAT")),
                                DatabaseIdentifiers.comparableIdentifier(rs.getString("TABLE_SCHEM")),
                                DatabaseIdentifiers.comparableIdentifier(rs.getString("TABLE_NAME"))
                        )
                );
            }
        }
        return tables;
    }

    private Map<String, Column> columnsOf(Table table) throws SQLException {
        Map<String, Column> columnMap = new HashMap<>();
        try (ResultSet rs = con.getMetaData().getColumns(
                DatabaseIdentifiers.comparableIdentifier(table.catalog),
                DatabaseIdentifiers.comparableIdentifier(table.schema),
                DatabaseIdentifiers.comparableIdentifier(table.name),
                null
        )) {
            while (rs.next()) {
                Column column = new Column(
                        table,
                        DatabaseIdentifiers.comparableIdentifier(rs.getString("COLUMN_NAME")),
                        rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable
                );
                columnMap.put(column.name, column);
            }
        }
        return columnMap;
    }

    private Set<String> primaryKeyColumns(Table table) throws SQLException {
        Set<String> columnNames = new HashSet<>();
        try (ResultSet rs = con.getMetaData().getPrimaryKeys(
                DatabaseIdentifiers.comparableIdentifier(table.catalog),
                DatabaseIdentifiers.comparableIdentifier(table.schema),
                DatabaseIdentifiers.comparableIdentifier(table.name))
        ) {
            while (rs.next()) {
                columnNames.add(
                        DatabaseIdentifiers.comparableIdentifier(rs.getString("COLUMN_NAME"))
                );
            }
        }
        return columnNames;
    }

    private Map<Set<String>, ForeignKey> foreignKeys(Table table) throws SQLException {
        Map<Tuple2<String, Table>, Map<String, String>> map = new HashMap<>();
        try (ResultSet rs = con.getMetaData().getImportedKeys(
                DatabaseIdentifiers.comparableIdentifier(table.catalog),
                DatabaseIdentifiers.comparableIdentifier(table.schema),
                DatabaseIdentifiers.comparableIdentifier(table.name)
        )) {
            while (rs.next()) {
                String constraintName = DatabaseIdentifiers.comparableIdentifier(rs.getString("FK_NAME"));
                Table referencedTable = tablesOf(
                        DatabaseIdentifiers.comparableIdentifier(rs.getString("PKTABLE_CAT")),
                        DatabaseIdentifiers.comparableIdentifier(rs.getString("PKTABLE_SCHEM")),
                        DatabaseIdentifiers.comparableIdentifier(rs.getString("PKTABLE_NAME"))
                ).iterator().next();
                String columnName = DatabaseIdentifiers.comparableIdentifier(rs.getString("FKCOLUMN_NAME"));
                String referencedColumnName = DatabaseIdentifiers.comparableIdentifier(rs.getString("PKCOLUMN_NAME"));
                map.computeIfAbsent(
                        new Tuple2<>(constraintName, referencedTable),
                        it -> new LinkedHashMap<>()).put(columnName, referencedColumnName
                );
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
            foreignKeyMap.put(columnNames, foreignKey);
        }
        return foreignKeyMap;
    }

    private static class Table {

        final String catalog;

        final String schema;

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

        public ForeignKey getForeignKey(ForeignKeyContext ctx, ColumnDefinition columnDefinition) throws SQLException{
            ForeignKey foreignKey;
            if (columnDefinition instanceof MultipleJoinColumns) {
                MultipleJoinColumns multipleJoinColumns = (MultipleJoinColumns) columnDefinition;
                Set<String> columnNames = new LinkedHashSet<>();
                for (int i = 0; i < multipleJoinColumns.size(); i++) {
                    columnNames.add(DatabaseIdentifiers.comparableIdentifier(multipleJoinColumns.name(i)));
                }
                foreignKey = getForeignKeyMap(ctx).get(columnNames);
            } else {
                foreignKey = getForeignKeyMap(ctx).get(
                        Collections.singleton(
                                DatabaseIdentifiers.comparableIdentifier(
                                        ((SingleColumn) columnDefinition).getName()
                                )
                        )
                );
            }
            if (foreignKey == null) {
                ctx.databaseValidators.items.add(
                        new DatabaseValidationException.Item(
                                ctx.type,
                                ctx.prop,
                                "No foreign key for columns: " + columnDefinition.toColumnNames()
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

        final String name;

        final boolean nullable;

        private Column(Table table, String name, boolean nullable) {
            this.table = table;
            this.name = name;
            this.nullable = nullable;
        }
    }

    private static class ForeignKey {

        final String constraintName;

        final Set<String> columnNames;

        final Table referencedTable;

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
            if (!referencedType.getIdProp().<ColumnDefinition>getStorage().toColumnNames().equals(referenceColumNames)) {
                ctx.databaseValidators.items.add(
                        new DatabaseValidationException.Item(
                                ctx.type,
                                ctx.prop,
                                "Illegal foreign key \"" +
                                        constraintName +
                                        "\", expected referenced columns are " +
                                        referenceColumNames +
                                        ", but actual referenced columns are " +
                                        referencedType.getIdProp().<ColumnDefinition>getStorage().toColumnNames()
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
}
