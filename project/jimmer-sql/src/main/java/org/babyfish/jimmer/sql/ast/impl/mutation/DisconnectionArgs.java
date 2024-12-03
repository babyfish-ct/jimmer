package org.babyfish.jimmer.sql.ast.impl.mutation;

import org.babyfish.jimmer.lang.Ref;

import java.util.Collection;

class DisconnectionArgs {

    final Collection<Object> deletedIds;

    final IdPairs retainedIdPairs;

    final ChildTableOperator caller;

    final boolean fireEvents;

    final Ref<Object> logicalDeletedValueRef;

    final boolean force;

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
        this.force = false;
    }

    DisconnectionArgs(DisconnectionArgs base, ChildTableOperator caller) {
        this.deletedIds = base.deletedIds;
        this.retainedIdPairs = base.retainedIdPairs;
        this.caller = caller;
        this.fireEvents = base.fireEvents;
        this.logicalDeletedValueRef = base.logicalDeletedValueRef;
        this.force = base.force;
    }

    private DisconnectionArgs(DisconnectionArgs base, boolean fireEvents) {
        this.deletedIds = base.deletedIds;
        this.retainedIdPairs = base.retainedIdPairs;
        this.caller = base.caller;
        this.fireEvents = fireEvents;
        this.logicalDeletedValueRef = base.logicalDeletedValueRef;
        this.force = base.force;
    }

    private DisconnectionArgs(DisconnectionArgs base, Ref<Object> logicalDeletedValueRef) {
        this.deletedIds = base.deletedIds;
        this.retainedIdPairs = base.retainedIdPairs;
        this.caller = base.caller;
        this.fireEvents = base.fireEvents;
        this.logicalDeletedValueRef = logicalDeletedValueRef;
        this.force = base.force;
    }

    private DisconnectionArgs(DisconnectionArgs base, int force) {
        this.deletedIds = base.deletedIds;
        this.retainedIdPairs = base.retainedIdPairs;
        this.caller = base.caller;
        this.fireEvents = base.fireEvents;
        this.logicalDeletedValueRef = base.logicalDeletedValueRef;
        this.force = force != 0;
    }

    DisconnectionArgs withCaller(ChildTableOperator caller) {
        if (this.caller == caller) {
            return this;
        }
        return new DisconnectionArgs(this, caller);
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

    DisconnectionArgs withForce(boolean force) {
        if (this.force == force) {
            return this;
        }
        return new DisconnectionArgs(this, force ? 1 : 0);
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

    static DisconnectionArgs retain(IdPairs.Retain idPairs, ChildTableOperator owner) {
        return new DisconnectionArgs(null, idPairs, owner);
    }
}
