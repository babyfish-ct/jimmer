package org.babyfish.jimmer.sql.kt.mutation

import org.babyfish.jimmer.kt.new
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.dialect.H2Dialect
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.embedded.Machine
import org.babyfish.jimmer.sql.kt.model.embedded.by
import org.junit.Test

class H2MutationTest : AbstractMutationTest() {

    @Test
    fun testInsertEmbeddedJson() {
        connectAndExpect({
            sqlClient { setDialect(H2Dialect()) }.entities.save(
                new(Machine::class).by {
                    id = 10L
                    location {
                        host = "localhost"
                        port = 8080
                    }
                    detail {
                        factories = mapOf(
                            "F-A" to "Factory-A",
                            "F-B" to "Factory-B"
                        )
                        patents = mapOf(
                            "P-I" to "Patent-I",
                            "P-II" to "Patent-II"
                        )
                    }
                },
                con = it
            ) {
                setMode(SaveMode.INSERT_ONLY)
            }.totalAffectedRowCount to sqlClient.entities.forConnection(it).findById(
                Machine::class,
                10L
            )
        }) {
            statement {
                sql(
                    """insert into MACHINE(ID, HOST, PORT, factory_map, patent_map) 
                    |values(?, ?, ?, ? format json, ? format json)""".trimMargin()
                )
            }
            statement {
                sql(
                    """select tb_1_.ID, tb_1_.HOST, tb_1_.PORT, tb_1_.factory_map, tb_1_.patent_map 
                        |from MACHINE tb_1_ 
                        |where tb_1_.ID = ?""".trimMargin()
                )
            }
            value(
                """(
                    |--->1, 
                    |--->{
                    |--->--->"id":10,
                    |--->--->"location":{
                    |--->--->--->"host":"localhost",
                    |--->--->--->"port":8080
                    |--->--->},
                    |--->--->"detail":{
                    |--->--->--->"factories":{"F-A":"Factory-A","F-B":"Factory-B"},
                    |--->--->--->"patents":{"P-I":"Patent-I","P-II":"Patent-II"}
                    |--->--->}
                    |--->}
                    |)""".trimMargin()
            )
        }
    }

    @Test
    fun testUpdateEmbeddedJson() {
        connectAndExpect({
            sqlClient { setDialect(H2Dialect()) }.entities.save(
                new(Machine::class).by {
                    id = 1L
                    detail().apply {
                        patents = mapOf(
                            "P-I" to "Patent-I",
                            "P-II" to "Patent-II"
                        )
                    }
                },
                con = it
            ) {
                setMode(SaveMode.UPDATE_ONLY)
            }.totalAffectedRowCount to sqlClient.entities.forConnection(it).findById(
                Machine::class,
                1L
            )
        }) {
            statement {
                sql(
                    """update MACHINE 
                        |set patent_map = ? format json 
                        |where ID = ?""".trimMargin()
                )
            }
            statement {
                sql(
                    """select tb_1_.ID, tb_1_.HOST, tb_1_.PORT, tb_1_.factory_map, tb_1_.patent_map 
                        |from MACHINE tb_1_ 
                        |where tb_1_.ID = ?""".trimMargin()
                )
            }
            value(
                """(
                    |--->1, 
                    |--->{
                    |--->--->"id":1,
                    |--->--->"location":{
                    |--->--->--->"host":"localhost",
                    |--->--->--->"port":8080
                    |--->--->},
                    |--->--->"detail":{
                    |--->--->--->"factories":{"f-1":"factory-1","f-2":"factory-2"},
                    |--->--->--->"patents":{"P-I":"Patent-I","P-II":"Patent-II"}
                    |--->--->}
                    |--->}
                    |)""".trimMargin()
            )
        }
    }
}