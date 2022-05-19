package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface RecursiveListLoader<E, T extends Table<E>> extends RecursiveLoader<E, T>, ListLoader<E, T> {

    @OldChain
    @Override
    RecursiveListLoader<E, T> filter(Filter<E, T> filter);

    @OldChain
    @Override
    RecursiveListLoader<E, T> batch(int size);

    @OldChain
    @Override
    RecursiveListLoader<E, T> limit(int limit);

    @OldChain
    RecursiveListLoader<E, T> depth(int depth);

    @OldChain
    RecursiveListLoader<E, T> recursive();

    @OldChain
    RecursiveListLoader<E, T> recursive(RecursionStrategy<E> strategy);
}
