package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface ListLoader<E, T extends Table<E>> extends Loader<E, T> {

    @OldChain
    @Override
    ListLoader<E, T> filter(Filter<E, T> filter);

    @OldChain
    @Override
    ListLoader<E, T> batch(int size);

    @OldChain
    ListLoader<E, T> limit(int limit);
}
