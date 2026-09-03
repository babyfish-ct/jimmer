package org.babyfish.jimmer.ddl.compiler

import site.addzero.util.db.DatabaseType
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseConnectionTimeoutTest {

    @Test
    fun `connection timeout keeps five seconds across driver units`() {
        listOf(
            DatabaseType.MYSQL,
            DatabaseType.OCEANBASE,
            DatabaseType.POLARDB,
            DatabaseType.TIDB,
        ).forEach { databaseType ->
            assertEquals("5000", connectionTimeout(databaseType))
        }
        assertEquals("5", connectionTimeout(DatabaseType.POSTGRESQL))
    }

    private fun connectionTimeout(databaseType: DatabaseType): String? =
        JimmerDdlCompilerSettings(databaseType = databaseType)
            .jdbcConnectionProperties()
            .getProperty("connectTimeout")
}
