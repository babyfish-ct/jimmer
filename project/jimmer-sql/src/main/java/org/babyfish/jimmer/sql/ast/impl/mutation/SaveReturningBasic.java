package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.jetbrains.annotations.Nullable;

class SaveReturningBasic {

    final Fetcher<?> fetcher;

    @Nullable
    final LogicalDeletedInfo logicalDeletedInfo;

    SaveReturningBasic(Fetcher<?> fetcher, @Nullable LogicalDeletedInfo logicalDeletedInfo) {
        this.fetcher = fetcher;
        this.logicalDeletedInfo = logicalDeletedInfo;
    }
}
