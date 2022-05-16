package org.babyfish.jimmer.sql.ast.table;

public interface AssociationTable<
        SE,
        ST extends Table<SE>,
        TE,
        TT extends Table<TE>
> extends Table<Association<SE, TE>> {

    @SuppressWarnings("unchecked")
    default ST source() {
        return (ST)join("source");
    }

    @SuppressWarnings("unchecked")
    default TT target() {
        return (TT)join("target");
    }
}
