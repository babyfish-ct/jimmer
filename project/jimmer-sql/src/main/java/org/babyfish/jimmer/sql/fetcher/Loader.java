package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.query.Filterable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.function.BiConsumer;

public interface Loader<E, T extends Table<E>> {

    @OldChain
    Loader<E, T> filter(BiConsumer<Filterable, T> block);

    @OldChain
    Loader<E, T> batch(int size);
}
