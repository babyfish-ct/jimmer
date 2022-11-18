package org.babyfish.jimmer.sql.kt

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.ImmutableProp
import org.babyfish.jimmer.meta.ImmutableType
import org.babyfish.jimmer.runtime.ImmutableSpi
import org.babyfish.jimmer.sql.association.meta.AssociationType
import org.babyfish.jimmer.sql.ast.table.Table
import org.babyfish.jimmer.sql.event.AssociationListener
import org.babyfish.jimmer.sql.event.EntityListener
import java.sql.Connection
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface KTriggers {

    fun <E: Any> addEntityListener(entityType: KClass<E>, listener: EntityListener<E>)

    fun <E: Any> removeEntityListener(entityType: KClass<E>, listener: EntityListener<E>)

    fun addEntityListener(immutableType: ImmutableType, listener: EntityListener<ImmutableSpi>)

    fun removeEntityListener(immutableType: ImmutableType, listener: EntityListener<ImmutableSpi>)

    fun addAssociationListener(
        prop: KProperty1<*, *>,
        listener: AssociationListener
    ) {
        addAssociationListener(prop.toImmutableProp(), listener)
    }

    fun <ST : Table<*>> removeAssociationListener(
        prop: KProperty1<*, *>,
        listener: AssociationListener
    ) {
        removeAssociationListener(prop.toImmutableProp(), listener)
    }

    fun addAssociationListener(prop: ImmutableProp, listener: AssociationListener)

    fun removeAssociationListener(prop: ImmutableProp, listener: AssociationListener)

    fun fireEntityTableChange(oldRow: Any, newRow: Any, con: Connection? = null, reason: Any? = null)

    fun fireMiddleTableDelete(prop: ImmutableProp, sourceId: Any, targetId: Any, con: Connection? = null, reason: Any? = null)

    fun fireMiddleTableInsert(prop: ImmutableProp, sourceId: Any, targetId: Any, con: Connection? = null, reason: Any? = null)

    fun fireAssociationEvict(prop: ImmutableProp, sourceId: Any, reason: Any? = null)
}