package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.query.Filterable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.function.BiConsumer;

public interface ListLoader<E, T extends Table<E>> extends Loader<E, T> {

    @OldChain
    @Override
    ListLoader<E, T> filter(BiConsumer<Filterable, T> block);

    @OldChain
    @Override
    ListLoader<E, T> batch(int size);

    @OldChain
    ListLoader<E, T> limit(int limit);
}
