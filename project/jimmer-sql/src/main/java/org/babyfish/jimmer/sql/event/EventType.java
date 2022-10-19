package org.babyfish.jimmer.sql.event;

public enum EventType {
    DELETE, // For EntityEvent and AssociationEvent
    INSERT, // For EntityEvent and AssociationEvent
    UPDATE, // Only for EntityEvent
    EVICT, // Only for AssociationEvent
}
