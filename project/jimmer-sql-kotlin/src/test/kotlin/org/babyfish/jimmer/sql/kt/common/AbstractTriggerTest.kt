package org.babyfish.jimmer.sql.kt.common

import org.babyfish.jimmer.meta.TargetLevel
import org.babyfish.jimmer.sql.event.*
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.cfg.KSqlClientDsl
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.expect
import kotlin.test.fail

abstract class AbstractTriggerTest : AbstractMutationTest() {

    protected val events = mutableListOf<String>()

    private var eventAsserted = false

    @BeforeTest
    fun initializeTriggerTest() {
        events.clear()
        eventAsserted = false
    }

    @AfterTest
    fun terminateTriggerTest() {
        if (!eventAsserted) {
            fail("`assertEvents` has not been called")
        }
    }

    override fun sqlClient(block: KSqlClientDsl.() -> Unit): KSqlClient =
        super.sqlClient {
            setTriggerType(TriggerType.TRANSACTION_ONLY)
            block()
        }.apply {
            for (type in javaClient.entityManager.getAllTypes("")) {
                if (type.isEntity()) {
                    getTriggers(true).addEntityListener(type) { e: EntityEvent<Any?> ->
                        events.add(
                            e.toString()
                        )
                    }
                    for (prop in type.getProps().values) {
                        if (prop.isAssociation(TargetLevel.PERSISTENT)) {
                            getTriggers(true).addAssociationListener(
                                prop
                            ) { e: AssociationEvent -> events.add(e.toString()) }
                        }
                    }
                }
            }
        }

    protected fun assertEvents(vararg events: String) {
        eventAsserted = true
        val expected = mutableListOf<String>()
        for (event in events) {
            expected.add(event.replace("--->", ""))
        }
        expect(events.map { it.replace("--->", "") }.sorted()) {
            this.events.sorted()
        }
    }
}

