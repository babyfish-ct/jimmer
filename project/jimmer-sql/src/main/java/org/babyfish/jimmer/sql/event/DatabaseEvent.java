package org.babyfish.jimmer.sql.event;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TypedProp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;

public interface DatabaseEvent {

    /**
     * Is the value of specified property changed.
     * @param prop The specified property
     * @return Is the value of specified property changed
     * <ul>
     *     <li>If the current event is {@link EntityEvent},
     *     returns whether the declaring type of specified property
     *     is assignable from the entity type of current event object
     *     and whether the value of specified property is changed.</li>
     *     <li>If the current event is {@link AssociationEvent}
     *     returns whether the specified property is {@link AssociationEvent#getImmutableProp()}</li>
     * </ul>
     */
    boolean isChanged(ImmutableProp prop);

    /**
     * Is the value of specified property changed
     * @param prop The specified property
     * @return Is the value of specified property changed
     * <ul>
     *     <li>If the current event is {@link EntityEvent},
     *     returns whether the declaring type of specified property
     *     is assignable from the entity type of current event object
     *     and whether the value of specified property is changed.</li>
     *     <li>If the current event is {@link AssociationEvent}
     *     returns whether the specified property is {@link AssociationEvent#getImmutableProp()}</li>
     * </ul>
     */
    boolean isChanged(TypedProp<?, ?> prop);

    /**
     * Determine whether the trigger for sending the current event is within
     * a transaction or based on binlog
     *
     * <ul>
     *  <li>If the event is fired by binlog trigger, returns null</li>
     *  <li>If the event is fired by transaction trigger, returns current trigger</li>
     * </ul>
     *
     * <p>
     *     Notes, If you use jimmer in spring-boot and accept events with `@EventListener`,
     *     it will be very important to determine whether this property is null.
     *     Because once the `triggerType` of `SqlClient` is set to `BOTH`, the same event
     *     will be notified twice.
     * </p>
     *
     * @return The current connection or null
     */
    @Nullable
    Connection getConnection();

    @Nullable
    Object getReason();

    boolean isEvict();
}
