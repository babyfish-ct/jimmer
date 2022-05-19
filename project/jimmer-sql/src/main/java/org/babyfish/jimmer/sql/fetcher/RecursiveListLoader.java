package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.query.Filterable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public interface RecursiveListLoader<E, T extends Table<E>> extends RecursiveLoader<E, T>, ListLoader<E, T> {

    @OldChain
    @Override
    RecursiveListLoader<E, T> filter(BiConsumer<Filterable, T> block);

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
