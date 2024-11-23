package org.babyfish.jimmer.sql.meta.impl;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.meta.impl.Utils;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class Storages {

    private Storages() {}

    public static Storage of(ImmutableProp prop, MetadataStrategy strategy) {
        if (!prop.hasStorage()) {
            return null;
        }
        DatabaseNamingStrategy namingStrategy = strategy.getNamingStrategy();
        Annotation annotation = prop.getAssociationAnnotation();
        if (annotation == null) {
            if (prop.isEmbedded(EmbeddedLevel.SCALAR)) {
                return new EmbeddedTree(prop).toEmbeddedColumns(strategy);
            }
            org.babyfish.jimmer.sql.Column column = prop.getAnnotation(org.babyfish.jimmer.sql.Column.class);
            String columnName = column != null ? column.name() : "";
            SqlTypeResult result = sqlType(prop, column, strategy);
            return new SingleColumn(
                    columnName.isEmpty() ?
                            namingStrategy.columnName(prop) :
                            Utils.resolveMetaString(columnName, strategy.getMetaStringResolver()),
                    false,
                    result.elementType,
                    result.type
            );
        }
        Storage storage = middleTable(prop, strategy, false);
        if (storage == null) {
            storage = joinColumn(prop, strategy, false);
        }
        if (storage == null) {
            if (prop.getAssociationAnnotation() instanceof ManyToMany) {
                storage = middleTable(prop, strategy, true);
            } else {
                storage = joinColumn(prop, strategy, true);
            }
        }
        return storage;
    }

    private static SqlTypeResult sqlType(ImmutableProp prop, Column column, MetadataStrategy strategy) {
        SqlTypeStrategy sqlTypeStrategy = strategy.getSqlTypeStrategy();
        ScalarTypeStrategy scalarTypeStrategy = strategy.getScalarTypeStrategy();
        if (column != null && !column.sqlType().isEmpty()) {
            if (prop.getReturnClass().isArray() || Collection.class.isAssignableFrom(prop.getReturnClass())) {
                throw new ModelException(
                        "Illegal property \"" +
                                prop +
                                "\", the \"sqlType\" of \"@" +
                                Column.class.getName() +
                                "\" cannot be set because is array or list"
                );
            }
            return new SqlTypeResult(null, column.sqlType());
        }
        if (column != null && !column.sqlElementType().isEmpty()) {
            if (!prop.getReturnClass().isArray() && !Collection.class.isAssignableFrom(prop.getReturnClass())) {
                throw new ModelException(
                        "Illegal property \"" +
                                prop +
                                "\", the \"sqlElementType\" of \"@" +
                                Column.class.getName() +
                                "\" cannot be set because is neither array nor collection"
                );
            }
            return new SqlTypeResult(
                    column.sqlElementType(),
                    column.sqlElementType() + sqlTypeStrategy.arrayTypeSuffix()
            );
        }
        Class<?> elementType = null;
        boolean isArray = false;
        Class<?> overriddenSqlClass = scalarTypeStrategy.getOverriddenSqlType(prop);
        if (overriddenSqlClass != null) {
            elementType = overriddenSqlClass;
        } else if (prop.getReturnClass().isArray()) {
            elementType = prop.getReturnClass().getComponentType();
            isArray = true;
        } else if (Collection.class.isAssignableFrom(prop.getReturnClass())) {
            Collection<Type> types = TypeUtils
                    .getTypeArguments((ParameterizedType) prop.getGenericType())
                    .values();
            if (!types.isEmpty()) {
                Type type = types.iterator().next();
                if (type instanceof Class<?>) {
                    elementType = (Class<?>) type;
                    isArray = true;
                }
            }
        } else {
            elementType = prop.getReturnClass();
        }
        if (elementType == null) {
            return SqlTypeResult.NIL;
        }
        elementType = Classes.primitiveTypeOf(elementType);
        String name = sqlTypeStrategy.sqlType(elementType);
        if (name == null) {
            return SqlTypeResult.NIL;
        }
        if (isArray) {
            return new SqlTypeResult(name, name + sqlTypeStrategy.arrayTypeSuffix());
        }
        return new SqlTypeResult(null, name);
    }

    private static ColumnDefinition joinColumn(
            ImmutableProp prop,
            MetadataStrategy strategy,
            boolean force
    ) {
        JoinColumn joinColumn = prop.getAnnotation(JoinColumn.class);
        JoinColumns joinColumns = prop.getAnnotation(JoinColumns.class);
        if (joinColumn == null && joinColumns == null && !force) {
            return null;
        }
        JoinColumnObj[] columns = joinColumns != null ?
                JoinColumnObj.array(prop, false, joinColumns.value(), strategy) :
                JoinColumnObj.array(prop, false, joinColumn, strategy);
        ColumnDefinition definition;
        try {
            definition = joinDefinition(columns, prop.getTargetType(), strategy);
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
                            "\" is illegal, it must reference one columns of target id " +
                            "(Now, foreign key references to non-id column is not supported)"
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
        SingleColumn targetIdColumn = columns == null || columns.length == 1 ?
                prop.getTargetType().getIdProp().getStorage(strategy) :
                null;
        return new SingleColumn(
                strategy.getNamingStrategy().foreignKeyColumnName(prop),
                columns != null ?
                        columns[0].isForeignKey :
                        isForeignKey(prop, false, ForeignKeyType.AUTO, strategy.getForeignKeyStrategy()),
                targetIdColumn != null ? targetIdColumn.getSqlElementType() : null,
                targetIdColumn != null ? targetIdColumn.getSqlType() : null
        );
    }

    private static MiddleTable middleTable(
            ImmutableProp prop,
            MetadataStrategy strategy,
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
            if (!joinTable.inverseJoinColumnName().isEmpty() && joinTable.inverseJoinColumns().length != 0) {
                throw new ModelException(
                        "Illegal property \"" +
                                prop +
                                "\", `inverseJoinColumnName` and `inverseJoinColumns` of `@" +
                                JoinTable.class.getName() +
                                "` cannot be specified at the same time"
                );
            }
            joinColumns = JoinColumnObj.array(prop, true, joinTable.joinColumnName(), strategy);
            if (joinColumns == null) {
                joinColumns = JoinColumnObj.array(prop, true, joinTable.joinColumns(), strategy);
            }
            inverseJoinColumns = JoinColumnObj.array(prop, false, joinTable.inverseJoinColumnName(), strategy);
            if (inverseJoinColumns == null) {
                inverseJoinColumns = JoinColumnObj.array(prop, false, joinTable.inverseJoinColumns(), strategy);
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
                    prop.getDeclaringType(),
                    strategy
            );
            leftParsed = true;
            targetDefinition = joinDefinition(
                    inverseJoinColumns,
                    prop.getTargetType(),
                    strategy
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
                            (leftParsed ? "inverseJoinColumns" : "joinColumns") +
                            "` must be specified when multiple `" +
                            (leftParsed ? "inverseJoinColumns" : "joinColumns") +
                            "` are used"
            );
        } catch (ReferenceNothing ex) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", the `referencedColumnName` \"" +
                            ex.ref +
                            "\" of `" +
                            (leftParsed ? "inverseJoinColumns" : "joinColumns") +
                            "` is illegal"
            );
        } catch (SourceConflict ex) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", conflict column name \"" +
                            ex.name +
                            "\" in several " +
                            (leftParsed ? "inverseJoinColumns" : "joinColumns")
            );
        } catch (TargetConflict ex) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", conflict referenced column name \"" +
                            ex.ref +
                            "\" in several " +
                            (leftParsed ? "inverseJoinColumns" : "joinColumns")
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
                            (leftParsed ? "inverseJoinColumns" : "joinColumns") +
                            ", their attribute `foreignKey` is different"
            );
        }
        String tableName = joinTable != null ? joinTable.name() : "";
        tableName = tableName.isEmpty() ?
                strategy.getNamingStrategy().middleTableName(prop) :
                Utils.resolveMetaString(tableName, strategy.getMetaStringResolver());

        SingleColumn sourceIdColumn = joinColumns == null || joinColumns.length == 1 ?
                prop.getDeclaringType().getIdProp().getStorage(strategy) :
                null;
        SingleColumn targetIdColumn = inverseJoinColumns == null || inverseJoinColumns.length == 1 ?
                prop.getTargetType().getIdProp().getStorage(strategy) :
                null;
        if (definition == null) {
            definition = new SingleColumn(
                    strategy.getNamingStrategy().middleTableBackRefColumnName(prop),
                    joinColumns != null ?
                            joinColumns[0].isForeignKey :
                            isForeignKey(prop, true, ForeignKeyType.AUTO, strategy.getForeignKeyStrategy()),
                    sourceIdColumn != null ? sourceIdColumn.getSqlElementType() : null,
                    sourceIdColumn != null ? sourceIdColumn.getSqlType() : null
            );
        }
        if (targetDefinition == null) {
            targetDefinition = new SingleColumn(
                    strategy.getNamingStrategy().middleTableTargetRefColumnName(prop),
                    inverseJoinColumns != null ?
                            inverseJoinColumns[0].isForeignKey :
                            isForeignKey(prop, false, ForeignKeyType.AUTO, strategy.getForeignKeyStrategy()),
                    targetIdColumn != null ? targetIdColumn.getSqlElementType() : null,
                    targetIdColumn != null ? targetIdColumn.getSqlType() : null
            );
        }
        boolean readonly = joinTable != null && joinTable.readonly();
        LogicalDeletedInfo logicalDeletedInfo = LogicalDeletedInfo.of(prop);
        JoinTableFilterInfo filterInfo = JoinTableFilterInfo.of(prop);
        if (joinTable != null && joinTable.deletedWhenEndpointIsLogicallyDeleted() && logicalDeletedInfo != null) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", the \"logicalDeletedFilter\" of \"@" +
                            JoinTable.class +
                            "\" has already been configured so that \"deletedWhenEndpointIsLogicallyDeleted\" cannot be true"
            );
        }
        if (!readonly && filterInfo != null && filterInfo.getValues().size() > 1) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", the \"values\" of \"@" +
                            JoinTable.JoinTableFilter.class.getName() +
                            "\" has multiple values so that the \"readonly\" of \"" +
                            JoinTable.class.getName() +
                            "\" must be true"
            );
        }
        return new MiddleTable(
                tableName,
                definition,
                targetDefinition,
                readonly,
                joinTable != null && joinTable.preventDeletionBySource(),
                joinTable != null && joinTable.preventDeletionByTarget(),
                joinTable != null && joinTable.cascadeDeletedBySource(),
                joinTable != null && joinTable.cascadeDeletedByTarget(),
                joinTable != null && joinTable.deletedWhenEndpointIsLogicallyDeleted(),
                logicalDeletedInfo,
                filterInfo
        );
    }

    private static ColumnDefinition joinDefinition(
            JoinColumnObj[] joinColumns,
            ImmutableType targetType,
            MetadataStrategy strategy
    ) throws IllegalJoinColumnCount, NoReference, ReferenceNothing, TargetConflict, SourceConflict, ForeignKeyConflict {
        if (joinColumns == null || joinColumns.length == 0) {
            ColumnDefinition definition = targetType.getIdProp().getStorage(strategy);
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
        ColumnDefinition targetIdDefinition = targetType.getIdProp().getStorage(strategy);
        if (joinColumns.length != targetIdDefinition.size()) {
            throw new IllegalJoinColumnCount(targetIdDefinition.size(), joinColumns.length);
        }
        if (joinColumns.length == 1) {
            String ref = joinColumns[0].referencedColumnName;
            if (!ref.isEmpty() && !ref.equals(targetIdDefinition.name(0))) {
                throw new ReferenceNothing(ref);
            }
            if (joinColumns[0].name.isEmpty()) {
                return null;
            }
            SingleColumn targetIdColumn = targetType.getIdProp().getStorage(strategy);
            return new SingleColumn(
                    joinColumns[0].name,
                    joinColumns[0].isForeignKey,
                    targetIdColumn != null ? targetIdColumn.getSqlElementType() : null,
                    targetIdColumn != null ? targetIdColumn.getSqlType() : null
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
                MetadataStrategy strategy
        ) {
            if (name.isEmpty()) {
                return null;
            }
            return new JoinColumnObj[] {
                    new JoinColumnObj(
                            Utils.resolveMetaString(name, strategy.getMetaStringResolver()),
                            "",
                            isForeignKey(prop, backRef, ForeignKeyType.AUTO, strategy.getForeignKeyStrategy())
                    )
            };
        }

        static JoinColumnObj[] array(
                ImmutableProp prop,
                boolean backRef,
                JoinColumn joinColumn,
                MetadataStrategy strategy
        ) {
            if (joinColumn == null) {
                return null;
            }
            return new JoinColumnObj[] {
                    new JoinColumnObj(
                            Utils.resolveMetaString(joinColumn.name(), strategy.getMetaStringResolver()),
                            Utils.resolveMetaString(joinColumn.referencedColumnName(), strategy.getMetaStringResolver()),
                            isForeignKey(prop, backRef, joinColumn.foreignKeyType(), strategy.getForeignKeyStrategy())
                    )
            };
        }

        static JoinColumnObj[] array(
                ImmutableProp prop,
                boolean backRef,
                JoinColumn[] arr,
                MetadataStrategy strategy
        ) {
            if (arr.length == 0) {
                return null;
            }
            return Arrays.stream(arr).map(it ->
                    new JoinColumnObj(
                            Utils.resolveMetaString(it.name(), strategy.getMetaStringResolver()),
                            Utils.resolveMetaString(it.referencedColumnName(), strategy.getMetaStringResolver()),
                            isForeignKey(prop, backRef, it.foreignKeyType(), strategy.getForeignKeyStrategy())
                    )
            ).toArray(JoinColumnObj[]::new);
        }
    }

    private static boolean isForeignKey(
            ImmutableProp prop,
            boolean backRef,
            ForeignKeyType foreignKeyType,
            ForeignKeyStrategy strategy
    ) {
        switch (foreignKeyType) {
            case REAL:
                if (strategy == ForeignKeyStrategy.FORCED_FAKE) {
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
                return strategy == ForeignKeyStrategy.REAL && (backRef || !prop.isRemote());
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

    private static class SqlTypeResult {

        static final SqlTypeResult NIL = new SqlTypeResult(null, null);

        final String elementType;

        final String type;

        SqlTypeResult(String elementType, String type) {
            this.elementType = elementType;
            this.type = type;
        }
    }
}
