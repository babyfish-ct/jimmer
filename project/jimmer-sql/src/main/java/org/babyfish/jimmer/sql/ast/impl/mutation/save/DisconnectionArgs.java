package org.babyfish.jimmer.sql.ast.impl.mutation.save;

import org.babyfish.jimmer.lang.Ref;

import java.util.Collection;

class DisconnectionArgs {

    final Collection<Object> deletedIds;

    final IdPairs retainedIdPairs;

    final ChildTableOperator caller;

    final boolean fireEvents;

    final Ref<Object> logicalDeletedValueRef;

    private DisconnectionArgs(
            Collection<Object> deleteIds,
            IdPairs retainedIdPairs,
            ChildTableOperator caller
    ) {
        this.deletedIds = deleteIds;
        this.retainedIdPairs = retainedIdPairs;
        this.caller = caller;
        this.fireEvents = false;
        this.logicalDeletedValueRef = null;
    }

    DisconnectionArgs(DisconnectionArgs base, ChildTableOperator caller) {
        this.deletedIds = base.deletedIds;
        this.retainedIdPairs = base.retainedIdPairs;
        this.caller = caller;
        this.fireEvents = base.fireEvents;
        this.logicalDeletedValueRef = base.logicalDeletedValueRef;
    }

    private DisconnectionArgs(DisconnectionArgs base, boolean fireEvents) {
        this.deletedIds = base.deletedIds;
        this.retainedIdPairs = base.retainedIdPairs;
        this.caller = base.caller;
        this.fireEvents = fireEvents;
        this.logicalDeletedValueRef = base.logicalDeletedValueRef;
    }

    private DisconnectionArgs(DisconnectionArgs base, Ref<Object> logicalDeletedValueRef) {
        this.deletedIds = base.deletedIds;
        this.retainedIdPairs = base.retainedIdPairs;
        this.caller = base.caller;
        this.fireEvents = base.fireEvents;
        this.logicalDeletedValueRef = logicalDeletedValueRef;
    }
    
    DisconnectionArgs withTrigger(boolean enabled) {
        if (fireEvents == enabled) {
            return this;
        }
        return new DisconnectionArgs(this, enabled);
    }

    DisconnectionArgs withLogicalDeletedValue(Object logicalDeletedValue) {
        return new DisconnectionArgs(this, Ref.of(logicalDeletedValue));
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
