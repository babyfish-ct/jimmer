package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.runtime.DraftSpi;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

class SaveSelfResult {

    final boolean detach;

    @Nullable
    final Set<DraftSpi> acceptedDrafts;

    SaveSelfResult(boolean detach, @Nullable Set<DraftSpi> acceptedDrafts) {
        this.detach = detach;
        this.acceptedDrafts = acceptedDrafts;
    }
}
