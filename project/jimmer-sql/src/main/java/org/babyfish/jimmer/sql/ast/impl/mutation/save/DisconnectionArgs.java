package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import java.util.Collection;

class DisconnectionArgs {

    final Collection<Object> deletedIds;

    final IdPairs retainedIdPairs;
    
    final boolean fireEvents;

    final ChildTableOperator caller;

    private DisconnectionArgs(
            Collection<Object> deleteIds,
            IdPairs retainedIdPairs,
            ChildTableOperator caller
    ) {
        this.deletedIds = deleteIds;
        this.retainedIdPairs = retainedIdPairs;
        this.fireEvents = false;
        this.caller = caller;
    }

    DisconnectionArgs(DisconnectionArgs base, ChildTableOperator caller) {
        this.deletedIds = base.deletedIds;
        this.retainedIdPairs = base.retainedIdPairs;
        this.fireEvents = base.fireEvents;
        this.caller = caller;
    }

    private DisconnectionArgs(DisconnectionArgs base, boolean fireEvents) {
        this.deletedIds = base.deletedIds;
        this.retainedIdPairs = base.retainedIdPairs;
        this.fireEvents = fireEvents;
        this.caller = base.caller;
    }
    
    DisconnectionArgs withTrigger(boolean enabled) {
        if (fireEvents == enabled) {
            return this;
        }
        return new DisconnectionArgs(this, enabled);
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
