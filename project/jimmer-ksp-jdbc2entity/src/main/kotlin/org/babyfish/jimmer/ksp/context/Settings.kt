package org.babyfish.jimmer.ksp.context

data class Settings(
    // 数据库相关配置
    // JDBC 配置
    val jdbcUrl: String = "",
    val jdbcUsername: String = "",
    val jdbcPassword: String = "",
    val jdbcSchema: String = "",
    val jdbcDriver: String = "",
    val modelSourceDir: String="",
    val modelPackageName: String="",
    val baseEntityPkg: String=""

) {
    companion object {
        fun toBean(map: Map<String, String>): Settings {
            return Settings(
                // JDBC 配置
                jdbcUrl = map["jdbcUrl"] ?: throw IllegalArgumentException(" jdbcUrl must be specified"),
                jdbcUsername = map["jdbcUsername"] ?: throw IllegalArgumentException("jdbcUsername must be specified"),
                jdbcPassword = map["jdbcPassword"] ?: throw IllegalArgumentException("jdbcPassword must be specified"),
                jdbcSchema = map["jdbcSchema"] ?: throw IllegalArgumentException("jdbcSchema must be specified"),
                jdbcDriver = map["jdbcDriver"] ?: throw IllegalArgumentException("jdbcDriver must be specified"),
                modelSourceDir = map["modelSourceDir"] ?: throw IllegalArgumentException("modelSourceDir dir must be specified"),
                modelPackageName = map["modelPackageName"] ?: throw IllegalArgumentException("modelPackageName name must be specified"),
                baseEntityPkg = map["baseEntityPkg"] ?: throw IllegalArgumentException("baseEntityPkg must be specified"),
            )

        }
    }

    fun toMap(): Map<String, String> {
        return mapOf(
            // JDBC 配置
            "jdbcUrl" to jdbcUrl,
            "jdbcUsername" to jdbcUsername,
            "jdbcPassword" to jdbcPassword,
            "jdbcSchema" to jdbcSchema,
            "jdbcDriver" to jdbcDriver,
            "modelSourceDir" to modelSourceDir,
            "modelPackageName" to modelPackageName,
            "baseEntityPkg" to baseEntityPkg,
        )
    }


    val modelOutputDir: String = "${modelSourceDir}/${modelPackageName.replace(".", "/")}"


}
