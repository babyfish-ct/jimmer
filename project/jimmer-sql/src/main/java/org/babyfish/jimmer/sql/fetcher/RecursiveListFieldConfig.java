package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface RecursiveListFieldConfig<E, T extends Table<E>> extends RecursiveFieldConfig<E, T>, ListFieldConfig<E, T> {

    @OldChain
    @Override
    RecursiveListFieldConfig<E, T> filter(FieldFilter<T> filter);

    @OldChain
    @Override
    RecursiveListFieldConfig<E, T> batch(int size);

    @OldChain
    @Override
    default RecursiveListFieldConfig<E, T> limit(int limit) {
        return limit(limit, 0);
    }

    @OldChain
    @Override
    RecursiveListFieldConfig<E, T> limit(int limit, int offset);

    @OldChain
    RecursiveListFieldConfig<E, T> depth(int depth);

    @OldChain
    RecursiveListFieldConfig<E, T> recursive();

    @OldChain
    RecursiveListFieldConfig<E, T> recursive(RecursionStrategy<E> strategy);
}
