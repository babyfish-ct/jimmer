package org.babyfish.jimmer.meta;

import org.babyfish.jimmer.sql.meta.Column;
import org.babyfish.jimmer.sql.meta.MiddleTable;
import org.babyfish.jimmer.sql.meta.Storage;

import javax.persistence.*;
import java.lang.annotation.Annotation;

public class Storages {

    static Storage of(ImmutableProp prop) {
        Annotation annotation = prop.getAssociationAnnotation();
        if (annotation instanceof OneToOne ||
                annotation instanceof OneToMany || (
                annotation instanceof ManyToMany &&
                        !((ManyToMany)annotation).mappedBy().isEmpty()
        )
        ) {
            return null;
        }
        if (annotation == null) {
            ImmutableProp idProp = prop.getDeclaringType().getIdProp();
            javax.persistence.Column column;
            if (prop.getName().equals(idProp.getName())) {
                column = idProp.getAnnotation(javax.persistence.Column.class);
            } else {
                column = prop.getAnnotation(javax.persistence.Column.class);
            }
            String columnName = column != null ? column.name() : "";
            if (columnName.isEmpty()) {
                columnName = Utils.databaseIdentifier(prop.getName());
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
                    Utils.databaseIdentifier(
                            prop.getDeclaringType().getJavaClass().getSimpleName()
                    ) +
                            '_' +
                            Utils.databaseIdentifier(
                                    prop.getElementClass().getSimpleName()
                            ) +
                            "_MAPPING";
        }
        String joinColumnName = joinTable != null && joinTable.joinColumns().length != 0 ?
                joinTable.joinColumns()[0].name() :
                "";
        if (joinColumnName.isEmpty()) {
            joinColumnName = ((Column)prop.getDeclaringType().getIdProp().getStorage()).getName();
        }
        String targetJoinColumn = joinTable != null && joinTable.inverseJoinColumns().length != 0 ?
                joinTable.inverseJoinColumns()[0].name() :
                "";
        if (targetJoinColumn.isEmpty()) {
            targetJoinColumn = Utils.databaseIdentifier(prop.getName()) + "_ID";
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
            columnName = Utils.databaseIdentifier(prop.getName()) + "_ID";
        }
        return new Column(columnName);
    }
}
