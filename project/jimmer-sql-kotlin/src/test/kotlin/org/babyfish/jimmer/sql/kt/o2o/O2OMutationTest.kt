package org.babyfish.jimmer.sql.kt.o2o

import org.babyfish.jimmer.sql.ast.impl.mutation.QueryReason
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.o2o.Customer
import org.babyfish.jimmer.sql.meta.impl.IdentityIdGenerator
import kotlin.test.Test

class O2OMutationTest : AbstractMutationTest() {

    @Test
    fun testSave() {
        val customer = Customer {
            name = "Alex"
            contact {
                email = "alex@gmail.com"
                address = "Street No 15, Raccoon town"
            }
        }
        executeAndExpectResult({con ->
            sqlClient {
                setIdGenerator(IdentityIdGenerator.INSTANCE)
                setDialect(H2Dialect())
            }.entities.forConnection(con).save(customer)
        }) {
            statement {
                sql(
                    """merge into CUSTOMER(NAME) 
                        |key(NAME) 
                        |values(?)""".trimMargin()
                )
            }
            statement {
                sql(
                    """merge into CONTACT(
                        |--->EMAIL, ADDRESS, CUSTOMER_ID
                        |) key(CUSTOMER_ID) values(?, ?, ?)""".trimMargin()
                )
            }
            statement {
                queryReason(QueryReason.CHECKING)
                sql(
                    """select tb_1_.ID 
                        |from CONTACT tb_1_ 
                        |where tb_1_.CUSTOMER_ID = ? and tb_1_.ID <> ? 
                        |limit ?""".trimMargin()
                )
            }
            entity {  }
        }
    }
}