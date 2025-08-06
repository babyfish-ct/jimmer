import org.babyfish.ksp.YmlUtil
import org.babyfish.ksp.YmlUtil.replaceEnvInString

/*
*
*这里是spring项目的一个从yml获取jdbc配置的示例(传递给ksp参数)
* 注意!!!   这里的参数是用了多数据源的配置,单数据源实际情况primary 用不到,常规读取单数据源的配置自行调整!
*
*    */
plugins {
    id("com.google.devtools.ksp")
}

//这里的项目以实际名称为主 通常是:backend:server
val serverProject = project(":backend:server")
val serverResourceDir = serverProject.projectDir.resolve("src/main/resources").absolutePath
val ymlPath = "${serverResourceDir}/application.yml"


val activate = YmlUtil.getActivate(ymlPath)
val ymlActivetePath = "${serverResourceDir}/application-${activate}.yml"
val ymlActiveConfig = YmlUtil.loadYmlConfigMap(ymlActivetePath)

val activeDatasource = YmlUtil.getConfigValue<String>(ymlActiveConfig, "spring.datasource.dynamic.primary")!!

val pgdatasource = YmlUtil.getConfigValue<Map<String, Any>>(ymlActiveConfig, "spring.datasource.dynamic.datasource")!!

val datasource = pgdatasource[activeDatasource] as Map<String, String>
println("datasource: $datasource")
val jdbcDriver = datasource["driver-class-name"]!!
val url = datasource["url"].replaceEnvInString()
val urlSplit = url.split("?")

val jdbcUrl = urlSplit.first()
val jdbcSchema = urlSplit.last().split("=").last()

val jdbcUsername = datasource["username"].replaceEnvInString()
val jdbcPassword = datasource["password"].replaceEnvInString()
val excludeTables = datasource["exclude-tables"].replaceEnvInString()


ksp {
//    必须: 同spring配置
    arg("jdbcUrl", jdbcUrl)
//    必须: 同spring配置
    arg("jdbcUsername", jdbcUsername)
//    必须: 同spring配置
    arg("jdbcPassword", jdbcPassword)
//    必须: 同spring配置
    arg("jdbcSchema", jdbcSchema)
//    必须: 同spring配置
    arg("jdbcDriver", jdbcDriver)
//    必须: 基字段: id 创建人 创建时间  更新人 更新时间
    arg("baseEntityPkg", "org.babyfish.model.common.BaseEntity")
    //必须: 生成的模型包路径
    arg("modelPackageName", "org.babyfish.model.entity")
    // 可选：指定要排除的表（逗号分隔）
    arg("excludeTables", excludeTables)



    println("jdbcUrl: $jdbcUrl")
    println("jdbcUsername: $jdbcUsername")
    println("jdbcPassword: $jdbcPassword")
    println("jdbcSchema: $jdbcSchema")
    println("jdbcDriver: $jdbcDriver")
    println("excludeTables: $excludeTables")
}
