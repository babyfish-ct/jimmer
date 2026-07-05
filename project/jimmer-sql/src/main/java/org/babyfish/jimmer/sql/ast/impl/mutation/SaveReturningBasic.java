package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.runtime.LogicalDeletedBehavior;
import org.jetbrains.annotations.Nullable;

class SaveReturningBasic {

    final Fetcher<?> fetcher;

    @Nullable
    final LogicalDeletedInfo logicalDeletedInfo;

    final LogicalDeletedBehavior logicalDeletedBehavior;

    SaveReturningBasic(
            Fetcher<?> fetcher,
            @Nullable LogicalDeletedInfo logicalDeletedInfo,
            LogicalDeletedBehavior logicalDeletedBehavior
    ) {
        this.fetcher = fetcher;
        this.logicalDeletedInfo = logicalDeletedInfo;
        this.logicalDeletedBehavior = logicalDeletedBehavior;
    }
}
