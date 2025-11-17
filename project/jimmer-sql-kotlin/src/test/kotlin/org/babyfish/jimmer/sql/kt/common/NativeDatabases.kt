package org.babyfish.jimmer.sql.kt.common

import org.springframework.jdbc.datasource.SimpleDriverDataSource
import java.sql.Driver
import javax.sql.DataSource


object NativeDatabases {

    fun isNativeAllowed(): Boolean {
        val nativeDb = System.getenv("jimmer-sql-test-native-database")
        return nativeDb != null && !nativeDb.isEmpty() && "false" != nativeDb
    }

    val MYSQL_DATA_SOURCE: DataSource by lazy {
        SimpleDriverDataSource(
            com.mysql.cj.jdbc.Driver(),
            "jdbc:mysql://localhost:3306/jimmer_test?useUnicode=true&characterEncoding=UTF-8",
            "root",
            "123456"
        )
    }
    val POSTGRES_DATA_SOURCE: DataSource by lazy {
        SimpleDriverDataSource(
            org.postgresql.Driver(),
            "jdbc:postgresql://localhost:5432/jimmer_test",
            "root",
            "123456"
        )
    }
}

