package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.runtime.DraftSpi;

import java.util.List;

class SaveOperation {

    final Saver saver;

    final List<DraftSpi> drafts;

    final PreHandler preHandler;

    final boolean ownerAcceptanceRequired;

    SaveOperation(
            Saver saver,
            List<DraftSpi> drafts,
            PreHandler preHandler,
            boolean ownerAcceptanceRequired
    ) {
        this.saver = saver;
        this.drafts = drafts;
        this.preHandler = preHandler;
        this.ownerAcceptanceRequired = ownerAcceptanceRequired;
    }
}
