package org.babyfish.jimmer.sql.kt.mutation.inheritance.singletable

import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.model.inheritance.enumdiscriminator.KEnumClient
import kotlin.test.Test

class SingleTableInstantiableRootMutationTest : AbstractMutationTest() {

    @Test
    fun testInsertRootBranch() {
        executeAndExpectResult({ con ->
            sqlClient.entities.forConnection(con).save(
                KEnumClient {
                    id = 320L
                    name = "Enum Root New"
                }
            ) {
                setMode(SaveMode.INSERT_ONLY)
            }
        }) {
            statement {
                sql(
                    "insert into K_ENUM_CLIENT(ID, CLIENT_TYPE, NAME) " +
                        "values(?, ?, ?)"
                )
                variables(320L, "CLIENT", "Enum Root New")
            }
            rowCount(KEnumClient::class, 1)
            entity {
                original("""{"id":320,"name":"Enum Root New"}""")
                modified("""{"id":320,"name":"Enum Root New"}""")
            }
        }
    }
}
