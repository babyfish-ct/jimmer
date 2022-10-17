package org.babyfish.jimmer.meta.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ModelException;
import org.babyfish.jimmer.sql.*;
import org.babyfish.jimmer.sql.meta.Column;
import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.babyfish.jimmer.sql.meta.Storage;

import java.lang.annotation.Annotation;

public class Storages {

    static Storage of(ImmutableProp prop) {
        if (prop.isTransient()) {
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
            org.babyfish.jimmer.sql.Column column = prop.getAnnotation(org.babyfish.jimmer.sql.Column.class);
            String columnName = column != null ? column.name() : "";
            if (columnName.isEmpty()) {
                columnName = DatabaseIdentifiers.databaseIdentifier(prop.getName());
            }
            return new Column(columnName);
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

    private static MiddleTable middleTable(ImmutableProp prop, boolean force) {
        JoinTable joinTable = prop.getAnnotation(JoinTable.class);
        if (joinTable == null && !force) {
            return null;
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
        String joinColumnName = joinTable != null ?
                joinTable.joinColumnName() :
                "";
        if (joinColumnName.isEmpty()) {
            joinColumnName =
                    DatabaseIdentifiers.databaseIdentifier(prop.getDeclaringType().getJavaClass().getSimpleName()) +
                            "_ID";
        }
        String targetJoinColumn = joinTable != null ?
                joinTable.inverseJoinColumnName() :
                "";
        if (targetJoinColumn.isEmpty()) {
            targetJoinColumn =
                    DatabaseIdentifiers.databaseIdentifier(prop.getTargetType().getJavaClass().getSimpleName()) +
                            "_ID";
        }
        if (joinColumnName.equals(targetJoinColumn)) {
            throw new ModelException(
                    "Illegal property \"" +
                            prop +
                            "\", please specify join columns and inverse join columns explicitly " +
                            "because they cannot be decided automatically"
            );
        }
        return new MiddleTable(tableName, joinColumnName, targetJoinColumn);
    }

    private static Column joinColumn(ImmutableProp prop, boolean force) {
        JoinColumn joinColumn = prop.getAnnotation(JoinColumn.class);
        if (joinColumn == null && !force) {
            return null;
        }
        String columnName = joinColumn != null ? joinColumn.name() : "";
        if (columnName.isEmpty()) {
            columnName = DatabaseIdentifiers.databaseIdentifier(prop.getName()) + "_ID";
        }
        return new Column(columnName);
    }
}
