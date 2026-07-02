package org.babyfish.jimmer.sql.ast.table;

import org.babyfish.jimmer.sql.ast.Predicate;

public interface PolymorphicTable<E> extends Table<E> {

    <T extends Table<?>> T treatAs(Class<T> tableType);

    <T extends Table<?>> T tryTreatAs(Class<T> tableType);

    Predicate instanceOf(Class<? extends E> type);

    Predicate exactType(Class<? extends E> type);
}
