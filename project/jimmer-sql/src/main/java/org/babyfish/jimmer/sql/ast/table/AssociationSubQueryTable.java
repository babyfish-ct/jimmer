package org.babyfish.jimmer.sql.ast.table;

public interface AssociationSubQueryTable<
        S,
        SST extends TableEx<S>,
        T,
        TST extends TableEx<T>
> extends Table<Association<S, T>> {

    @SuppressWarnings("unchecked")
    default SST source() {
        return (SST)join("source");
    }

    @SuppressWarnings("unchecked")
    default TST target() {
        return (TST)join("target");
    }
}
