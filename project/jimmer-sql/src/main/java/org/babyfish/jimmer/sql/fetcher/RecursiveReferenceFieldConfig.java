package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface RecursiveReferenceFieldConfig<E, T extends Table<E>> extends RecursiveFieldConfig<E, T>, ReferenceFieldConfig<E, T> {

    @OldChain
    @Override
    RecursiveReferenceFieldConfig<E, T> filter(FieldFilter<T> filter);

    @OldChain
    @Override
    RecursiveReferenceFieldConfig<E, T> batch(int size);

    @OldChain
    @Override
    RecursiveReferenceFieldConfig<E, T> fetchType(ReferenceFetchType fetchType);

    @OldChain
    @Override
    RecursiveReferenceFieldConfig<E, T> depth(int depth);

    @OldChain
    @Override
    RecursiveReferenceFieldConfig<E, T> recursive(RecursionStrategy<E> strategy);
}
