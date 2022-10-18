package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.lang.OldChain;
import org.babyfish.jimmer.sql.ast.table.Table;

public interface ListFieldConfig<E, T extends Table<E>> extends FieldConfig<E, T> {

    @OldChain
    @Override
    ListFieldConfig<E, T> filter(FieldFilter<T> filter);

    @OldChain
    @Override
    ListFieldConfig<E, T> batch(int size);

    @OldChain
    default ListFieldConfig<E, T> limit(int limit) {
        return limit(limit, 0);
    }

    @OldChain
    ListFieldConfig<E, T> limit(int limit, int offset);
}
