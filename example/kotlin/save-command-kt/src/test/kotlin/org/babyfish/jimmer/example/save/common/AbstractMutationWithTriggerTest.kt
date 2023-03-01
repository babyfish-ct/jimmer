package org.babyfish.jimmer.example.save.common

import org.babyfish.jimmer.sql.event.TriggerType
import org.babyfish.jimmer.sql.kt.KSqlClientDsl
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import kotlin.math.min

abstract class AbstractMutationWithTriggerTest : AbstractMutationTest() {

    private val events: MutableList<String> = ArrayList()

    override fun customize(dsl: KSqlClientDsl) {
        dsl.setTriggerType(TriggerType.TRANSACTION_ONLY)
    }

    @BeforeEach
    fun registerEventListeners() {

        events.clear()

        /*
         * If jimmer-spring-starter is used, it's unnecessary
         * to use triggers to register listeners explicitly,
         * because jimmer trigger events will be published
         * as spring events automatically
         */
        sql.triggers.addEntityListener { e ->
            events.add(
                "The entity \"" +
                    e.immutableType +
                    "\" is changed, " +
                    "old: " + e.oldEntity +
                    ", new: " + e.newEntity
            )
        }
        sql.triggers.addAssociationListener { e ->
            events.add(
                "The association \"" +
                    e.immutableProp.toString() +
                    "\" is changed, " +
                    "source id: " + e.sourceId +
                    ", detached target id: " + e.detachedTargetId +
                    ", attached target id: " + e.attachedTargetId
            )
        }
    }

    protected fun assertEvents(vararg events: String) {
        val size = min(this.events.size, events.size)
        for (i in 0 until size) {
            Assertions.assertEquals(
                events[i],
                this.events[i],
                "events[i]: expected \"" +
                    events[i] +
                    "\", actual \"" +
                    this.events[i] +
                    "\""
            )
        }
        Assertions.assertEquals(
            events.size,
            this.events.size,
            ("Event count: expected " +
                events.size +
                ", actual " +
                this.events.size)
        )
    }
}