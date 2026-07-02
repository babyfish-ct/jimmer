package org.babyfish.jimmer.sql.kt.mutation.inheritance.singletable

import org.babyfish.jimmer.sql.ast.mutation.TypeMatchMode
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.KClient
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.id
import org.babyfish.jimmer.sql.kt.model.inheritance.singletable.name
import kotlin.test.Test

class SingleTableInheritanceDMLTest : AbstractMutationTest() {

    @Test
    fun testUpdateRootTypePolymorphically() {
        executeAndExpectRowCount(
            sqlClient.createUpdate(KClient::class) {
                setTypeMatchMode(TypeMatchMode.POLYMORPHIC)
                set(table.name, "Client+")
                where(table.id valueIn listOf(100L, 101L))
            }
        ) {
            statement {
                sql(
                    "update CLIENT tb_1_ " +
                        "set NAME = ? " +
                        "where tb_1_.ID in (?, ?)"
                )
                variables("Client+", 100L, 101L)
            }
            rowCount(2)
        }
    }
}
