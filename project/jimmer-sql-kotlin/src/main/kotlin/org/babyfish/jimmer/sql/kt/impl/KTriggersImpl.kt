package org.babyfish.jimmer.sql.kt.impl

import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.runtime.ImmutableSpi
import org.babyfish.jimmer.sql.event.Triggers
import org.babyfish.jimmer.sql.event.AssociationListener
import org.babyfish.jimmer.sql.event.EntityListener
import org.babyfish.jimmer.sql.kt.KTriggers
import java.sql.Connection
import kotlin.reflect.KClass

internal class KTriggersImpl(
    private val javaTriggers: Triggers
): KTriggers {

    override fun <E : Any> addEntityListener(entityType: KClass<E>, listener: EntityListener<E>) {
        javaTriggers.addEntityListener(entityType.java, listener)
    }

    override fun addEntityListener(immutableType: ImmutableType, listener: EntityListener<ImmutableSpi>) {
        javaTriggers.addEntityListener(immutableType, listener)
    }

    override fun <E : Any> removeEntityListener(entityType: KClass<E>, listener: EntityListener<E>) {
        javaTriggers.removeEntityListener(entityType.java, listener)
    }

    override fun removeEntityListener(immutableType: ImmutableType, listener: EntityListener<ImmutableSpi>) {
        javaTriggers.removeEntityListener(immutableType, listener)
    }

    override fun addAssociationListener(prop: ImmutableProp, listener: AssociationListener) {
        javaTriggers.addAssociationListener(prop, listener)
    }

    override fun removeAssociationListener(prop: ImmutableProp, listener: AssociationListener) {
        javaTriggers.removeAssociationListener(prop, listener)
    }

    override fun fireEntityTableChange(oldRow: Any, newRow: Any, con: Connection?, reason: Any?) {
        javaTriggers.fireEntityTableChange(oldRow, newRow, con, reason)
    }

    override fun fireMiddleTableDelete(prop: ImmutableProp, sourceId: Any, targetId: Any, con: Connection?, reason: Any?) {
        javaTriggers.fireMiddleTableDelete(prop, sourceId, targetId, con, reason)
    }

    override fun fireMiddleTableInsert(prop: ImmutableProp, sourceId: Any, targetId: Any, con: Connection?, reason: Any?) {
        javaTriggers.fireMiddleTableInsert(prop, sourceId, targetId, con, reason)
    }

    override fun fireAssociationEvict(prop: ImmutableProp, sourceId: Any, reason: Any?) {
        javaTriggers.fireAssociationEvict(prop, sourceId, reason);
    }
}