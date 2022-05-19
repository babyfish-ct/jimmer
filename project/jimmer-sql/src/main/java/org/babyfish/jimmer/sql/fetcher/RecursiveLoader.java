package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.query.Filterable;
import org.babyfish.jimmer.sql.ast.table.Table;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public interface RecursiveLoader<E, T extends Table<E>> extends Loader<E, T> {

    @OldChain
    @Override
    RecursiveLoader<E, T> filter(BiConsumer<Filterable, T> block);

    @OldChain
    @Override
    RecursiveLoader<E, T> batch(int size);

    @OldChain
    RecursiveLoader<E, T> depth(int depth);

    @OldChain
    RecursiveLoader<E, T> recursive();

    @OldChain
    RecursiveLoader<E, T> recursive(RecursionStrategy<E> strategy);
}
