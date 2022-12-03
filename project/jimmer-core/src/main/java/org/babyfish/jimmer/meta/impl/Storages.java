package org.babyfish.jimmer.meta.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.ModelException;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.*;

import javax.xml.stream.events.StartDocument;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Storages {

    static Storage of(ImmutableProp prop) {
        if (prop.isTransient() || prop.getDeclaringType().isEmbeddable()) {
            return null;
        }
        Annotation annotation = prop.getAssociationAnnotation();
        if (annotation instanceof OneToOne && !((OneToOne)annotation).mappedBy().isEmpty() ||
                annotation instanceof OneToMany || (
                annotation instanceof ManyToMany && !((ManyToMany)annotation).mappedBy().isEmpty())
        ) {
            return null;
        }
        if (annotation == null) {
            if (prop.isEmbedded()) {
                return new EmbeddedTree(prop).toEmbeddedColumns();
            }
            org.babyfish.jimmer.sql.Column column = prop.getAnnotation(org.babyfish.jimmer.sql.Column.class);
            String columnName = column != null ? column.name() : "";
            if (columnName.isEmpty()) {
                columnName = DatabaseIdentifiers.databaseIdentifier(prop.getName());
            }
            return new SingleColumn(columnName);
        }
        Storage storage = middleTable(prop, false);
        if (storage == null) {
            storage = joinColumn(prop, false);
        }
        if (storage == null) {
            if (prop.getAssociationAnnotation() instanceof ManyToMany) {
                storage = middleTable(prop, true);
            } else {
                storage = joinColumn(prop, true);
            }
        }
        return storage;
    }

    private static ColumnDefinition joinColumn(ImmutableProp prop, boolean force) {
        JoinColumn joinColumn = prop.getAnnotation(JoinColumn.class);
        JoinColumns joinColumns = prop.getAnnotation(JoinColumns.class);
        if (joinColumn == null && joinColumns == null && !force) {
            return null;
        }
        JoinColumnObj[] columns = joinColumns != null ?
                JoinColumnObj.array(joinColumns.value()) :
                JoinColumnObj.array(joinColumn);
        ColumnDefinition definition;
        try {
            definition= joinDefinition(columns, prop.getTargetType());
        } catch (IllegalJoinColumnCount ex) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", it has " +
                            ex.actual +
                            " join columns, but the referenced property \"" +
                            prop.getTargetType().getIdProp() +
                            "\" has " +
                            ex.expect +
                            " join columns"
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
        }
        if (definition != null) {
            return definition;
        }
        return new SingleColumn(
                DatabaseIdentifiers.databaseIdentifier(prop.getName()) + "_ID"
        );
    }

    private static MiddleTable middleTable(ImmutableProp prop, boolean force) {
        JoinTable joinTable = prop.getAnnotation(JoinTable.class);
        if (joinTable == null && !force) {
            return null;
        }
        JoinColumnObj[] joinColumns;
        JoinColumnObj[] referencedColumns;
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
            joinColumns = JoinColumnObj.array(joinTable.joinColumnName());
            if (joinColumns == null) {
                joinColumns = JoinColumnObj.array(joinTable.joinColumns());
            }
            referencedColumns = JoinColumnObj.array(joinTable.inverseJoinColumnName());
            if (referencedColumns == null) {
                referencedColumns = JoinColumnObj.array(joinTable.inverseColumns());
            }
        } else {
            joinColumns = null;
            referencedColumns = null;
        }
        ColumnDefinition definition;
        ColumnDefinition targetDefinition;
        boolean leftParsed = false;
        try {
            definition = joinDefinition(joinColumns, prop.getDeclaringType());
            leftParsed = true;
            targetDefinition = joinDefinition(referencedColumns, prop.getTargetType());
        } catch (IllegalJoinColumnCount ex) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", there are " +
                            ex.actual +
                            "`" +
                            (leftParsed ? "inverseColumns" : "joinColumns") +
                            "`, but the id property \"" +
                            (leftParsed ? prop.getTargetType() : prop.getDeclaringType()).getIdProp() +
                            "\" has " +
                            ex.expect +
                            "column(s)"
            );
        } catch (NoReference ex) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", the `referencedColumns` of `" +
                            (leftParsed ? "inverseColumns" : "joinColumns") +
                            "`must be specified when multiple `" +
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
                            "`is illegal"
            );
        }
        String tableName = joinTable != null ? joinTable.name() : "";
        if (tableName.isEmpty()) {
            tableName =
                    DatabaseIdentifiers.databaseIdentifier(
                            prop.getDeclaringType().getJavaClass().getSimpleName()
                    ) +
                            '_' +
                            DatabaseIdentifiers.databaseIdentifier(
                                    prop.getElementClass().getSimpleName()
                            ) +
                            "_MAPPING";
        }

        if (definition == null) {
            definition = new SingleColumn(
                    DatabaseIdentifiers.databaseIdentifier(prop.getDeclaringType().getJavaClass().getSimpleName()) +
                            "_ID"
            );
        }
        if (targetDefinition == null) {
            targetDefinition = new SingleColumn(
                    DatabaseIdentifiers.databaseIdentifier(prop.getTargetType().getJavaClass().getSimpleName()) +
                            "_ID"
            );
        }
        String conflictColumnName = null;
        for (String name : definition) {
            if (targetDefinition.contains(name)) {
                conflictColumnName = name;
                break;
            }
        }
        if (conflictColumnName == null) {
            for (String name : targetDefinition) {
                if (definition.contains(name)) {
                    conflictColumnName = name;
                    break;
                }
            }
        }
        if (conflictColumnName != null) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", there is a conflict column \"" +
                            conflictColumnName +
                            "\" in the middle table \"" +
                            tableName +
                            "\""
            );
        }
        return new MiddleTable(tableName, definition, targetDefinition);
    }

    private static ColumnDefinition joinDefinition(
            JoinColumnObj[] joinColumns,
            ImmutableType targetType
    ) throws IllegalJoinColumnCount, NoReference, ReferenceNothing {
        if (joinColumns == null || joinColumns.length == 0) {
            return null;
        }
        ColumnDefinition targetIdDefinition = targetType.getIdProp().getStorage();
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
            return new SingleColumn(joinColumns[0].name);
        }
        Map<String, String> columnMap = new HashMap<>();
        for (JoinColumnObj joinColumn : joinColumns) {
            String ref = joinColumn.referencedColumnName;
            if (ref.isEmpty()) {
                throw new NoReference();
            }
            if (!targetIdDefinition.contains(ref)) {
                throw new ReferenceNothing(ref);
            }
            columnMap.put(ref, joinColumn.name);
        }
        Map<String, String> referencedColumnMap = new LinkedHashMap<>();
        for (String targetColumnName : targetIdDefinition) {
            referencedColumnMap.put(columnMap.get(targetColumnName), targetColumnName);
        }
        return new MultipleJoinColumns(referencedColumnMap);
    }

    private static class JoinColumnObj {

        final String name;

        final String referencedColumnName;

        JoinColumnObj(String name, String referencedColumnName) {
            this.name = name;
            this.referencedColumnName = referencedColumnName;
        }

        JoinColumnObj(JoinColumn joinColumn) {
            this.name = joinColumn.name();
            this.referencedColumnName = joinColumn.referencedColumnName();
        }

        static JoinColumnObj[] array(String name) {
            if (name.isEmpty()) {
                return null;
            }
            return new JoinColumnObj[] {
                    new JoinColumnObj(name, "")
            };
        }

        static JoinColumnObj[] array(JoinColumn joinColumn) {
            if (joinColumn == null) {
                return null;
            }
            return new JoinColumnObj[] {
                    new JoinColumnObj(
                            joinColumn.name(),
                            joinColumn.referencedColumnName()
                    )
            };
        }

        static JoinColumnObj[] array(JoinColumn[] arr) {
            if (arr.length == 0) {
                return null;
            }
            return Arrays.stream(arr).map(JoinColumnObj::new).toArray(JoinColumnObj[]::new);
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

        private ReferenceNothing(String ref) {
            this.ref = ref;
        }
    }
}

