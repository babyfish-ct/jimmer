package org.babyfish.jimmer.sql.ast.table;

public interface AssociationTable<
        S,
        ST extends Table<S>,
        T,
        TT extends Table<T>
> extends Table<Association<S, T>> {

    @SuppressWarnings("unchecked")
    default ST source() {
        return (ST)join("source");
    }

    @SuppressWarnings("unchecked")
    default TT target() {
        return (TT)join("target");
    }
}
