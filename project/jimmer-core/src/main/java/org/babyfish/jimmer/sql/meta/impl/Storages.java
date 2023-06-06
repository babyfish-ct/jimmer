package org.babyfish.jimmer.sql.meta.impl;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.ModelException;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.*;

import java.lang.annotation.Annotation;
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
                return new EmbeddedTree(prop).toEmbeddedColumns(namingStrategy);
            }
            org.babyfish.jimmer.sql.Column column = prop.getAnnotation(org.babyfish.jimmer.sql.Column.class);
            String columnName = column != null ? column.name() : "";
            if (columnName.isEmpty()) {
                columnName = namingStrategy.columnName(prop);
            }
            return new SingleColumn(columnName, false);
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
        DatabaseNamingStrategy namingStrategy = strategy.getNamingStrategy();
        ForeignKeyStrategy foreignKeyStrategy = strategy.getForeignKeyStrategy();
        JoinColumnObj[] columns = joinColumns != null ?
                JoinColumnObj.array(prop, false, joinColumns.value(), foreignKeyStrategy) :
                JoinColumnObj.array(prop, false, joinColumn, foreignKeyStrategy);
        ColumnDefinition definition;
        try {
            definition= joinDefinition(columns, prop.getTargetType(), strategy);
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
                namingStrategy.foreignKeyColumnName(prop),
                isForeignKey(prop, false, ForeignKeyType.AUTO, foreignKeyStrategy)
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
        DatabaseNamingStrategy namingStrategy = strategy.getNamingStrategy();
        ForeignKeyStrategy foreignKeyStrategy = strategy.getForeignKeyStrategy();
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
            joinColumns = JoinColumnObj.array(prop, true, joinTable.joinColumnName(), foreignKeyStrategy);
            if (joinColumns == null) {
                joinColumns = JoinColumnObj.array(prop, true, joinTable.joinColumns(), foreignKeyStrategy);
            }
            inverseJoinColumns = JoinColumnObj.array(prop, false, joinTable.inverseJoinColumnName(), foreignKeyStrategy);
            if (inverseJoinColumns == null) {
                inverseJoinColumns = JoinColumnObj.array(prop, false, joinTable.inverseColumns(), foreignKeyStrategy);
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
            tableName = namingStrategy.middleTableName(prop);
        }

        if (definition == null) {
            definition = new SingleColumn(
                    namingStrategy.middleTableBackRefColumnName(prop),
                    isForeignKey(prop, true, ForeignKeyType.AUTO, foreignKeyStrategy)
            );
        }
        if (targetDefinition == null) {
            targetDefinition = new SingleColumn(
                    namingStrategy.middleTableTargetRefColumnName(prop),
                    isForeignKey(prop, false, ForeignKeyType.AUTO, foreignKeyStrategy)
            );
        }
        return new MiddleTable(
                tableName,
                definition,
                targetDefinition,
                joinTable != null && joinTable.preventDeletionBySource(),
                joinTable != null && joinTable.preventDeletionByTarget()
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
                ForeignKeyStrategy strategy
        ) {
            if (name.isEmpty()) {
                return null;
            }
            return new JoinColumnObj[] {
                    new JoinColumnObj(
                            name,
                            "",
                            isForeignKey(prop, backRef, ForeignKeyType.AUTO, strategy)
                    )
            };
        }

        static JoinColumnObj[] array(
                ImmutableProp prop,
                boolean backRef,
                JoinColumn joinColumn,
                ForeignKeyStrategy strategy
        ) {
            if (joinColumn == null) {
                return null;
            }
            return new JoinColumnObj[] {
                    new JoinColumnObj(
                            joinColumn.name(),
                            joinColumn.referencedColumnName(),
                            isForeignKey(prop, backRef, joinColumn.foreignKeyType(), strategy)
                    )
            };
        }

        static JoinColumnObj[] array(
                ImmutableProp prop,
                boolean backRef,
                JoinColumn[] arr,
                ForeignKeyStrategy strategy
        ) {
            if (arr.length == 0) {
                return null;
            }
            return Arrays.stream(arr).map(it ->
                    new JoinColumnObj(
                            it.name(),
                            it.referencedColumnName(),
                            isForeignKey(prop, backRef, it.foreignKeyType(), strategy)
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
}
