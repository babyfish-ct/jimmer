package org.babyfish.jimmer.sql.kt.mutation

import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.dialect.PostgresDialect
import org.babyfish.jimmer.sql.kt.common.AbstractMutationTest
import org.babyfish.jimmer.sql.kt.common.NativeDatabases
import org.babyfish.jimmer.sql.kt.model.bug751.NullableBool
import org.babyfish.jimmer.sql.runtime.DbLiteral.DbNull
import org.junit.Assume
import kotlin.test.Test

class NullableBoolTest : AbstractMutationTest() {

    @Test
    fun test() {
        Assume.assumeTrue(NativeDatabases.isNativeAllowed())
        connectAndExpect(
            NativeDatabases.POSTGRES_DATA_SOURCE,
            {
                sqlClient {
                    setDialect(PostgresDialect())
                }.entities.forConnection(it).save(NullableBool {
                    id = 1L
                    value = null
                }) {
                    setMode(SaveMode.INSERT_ONLY)
                }.totalAffectedRowCount
            }
        ) {
            statement {
                sql(
                    """insert into NULLABLE_BOOL(ID, VALUE) values(?, ?)"""
                )
                variables(1L, DbNull(Boolean::class.javaObjectType))
            }
        }
    }
}