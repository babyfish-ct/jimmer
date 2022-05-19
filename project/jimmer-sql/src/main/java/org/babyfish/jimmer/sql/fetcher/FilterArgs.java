package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.Collection;

public interface FilterArgs<E, T extends Table<E>> extends Sortable {

    T getTable();

    <K> K getKey();

    <K> Collection<K> getKeys();
}
