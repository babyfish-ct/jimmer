package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import java.util.Collection;

class DisconnectionArgs {

    final Collection<Object> deletedIds;

    final IdPairs retainedIdPairs;

    final ChildTableOperator caller;

    private DisconnectionArgs(
            Collection<Object> deleteIds,
            IdPairs retainedIdPairs,
            ChildTableOperator caller
    ) {
        this.deletedIds = deleteIds;
        this.retainedIdPairs = retainedIdPairs;
        this.caller = caller;
    }

    DisconnectionArgs(DisconnectionArgs base, ChildTableOperator caller) {
        this.deletedIds = base.deletedIds;
        this.retainedIdPairs = base.retainedIdPairs;
        this.caller = caller;
    }

    boolean isEmpty() {
        if (deletedIds != null) {
            return deletedIds.isEmpty();
        }
        return retainedIdPairs.isEmpty();
    }

    static DisconnectionArgs delete(Collection<Object> ids, ChildTableOperator owner) {
        return new DisconnectionArgs(ids, null, owner);
    }

    static DisconnectionArgs retain(IdPairs idPairs, ChildTableOperator owner) {
        return new DisconnectionArgs(null, idPairs, owner);
    }
}
