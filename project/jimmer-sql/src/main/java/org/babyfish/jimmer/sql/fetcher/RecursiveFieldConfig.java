package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface RecursiveFieldConfig<E, T extends Table<E>> extends FieldConfig<E, T> {

    @OldChain
    @Override
    RecursiveFieldConfig<E, T> filter(FieldFilter<T> filter);

    @OldChain
    @Override
    RecursiveFieldConfig<E, T> batch(int size);

    @OldChain
    RecursiveFieldConfig<E, T> depth(int depth);

    @OldChain
    RecursiveFieldConfig<E, T> recursive();

    @OldChain
    RecursiveFieldConfig<E, T> recursive(RecursionStrategy<E> strategy);
}
