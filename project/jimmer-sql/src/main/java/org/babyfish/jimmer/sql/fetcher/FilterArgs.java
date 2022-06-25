package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.sql.ast.query.Sortable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.Collection;

public interface FilterArgs<T extends Table<?>> extends Sortable {

    T getTable();

    <K> K getKey();

    <K> Collection<K> getKeys();
}
