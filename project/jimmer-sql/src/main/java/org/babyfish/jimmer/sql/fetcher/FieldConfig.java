package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface FieldConfig<E, T extends Table<E>> {

    @OldChain
    FieldConfig<E, T> filter(FieldFilter<T> filter);

    @OldChain
    FieldConfig<E, T> batch(int size);
}
