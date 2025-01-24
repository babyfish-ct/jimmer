package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface ReferenceFieldConfig<E, T extends Table<E>> extends FieldConfig<E, T> {

    @OldChain
    @Override
    ReferenceFieldConfig<E, T> batch(int size);

    @OldChain
    @Override
    ReferenceFieldConfig<E, T> filter(FieldFilter<T> filter);

    @OldChain
    ReferenceFieldConfig<E, T> fetchType(ReferenceFetchType fetchType);
}
