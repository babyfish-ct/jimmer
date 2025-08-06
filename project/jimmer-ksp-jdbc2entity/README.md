# 读取jdbc元数据生成模型(控制台可以看到具体的日志,已生成的文件会跳过,不会覆盖用户二次修改的内容)
```kotlin

//该模块所需的ksp参数,可以从spring上下文激活的yml文件读取数据库配置(见buildSrc中的ksp4jdbc.gradle.kts)
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
    //我们通常把模型生成在这个目录(ksp处理器jimmer-ksp-jdbc2entity 需要modelSourceDir, 以配置模型生成的源码目录)
    arg("modelSourceDir", modelSourceDir)
    // 可选：指定要排除的表（逗号分隔）
    arg("excludeTables", excludeTables)

}

```
# 在用户的代码中(以多模块项目 backend:model, backend:server为例    )
# 在toml  [libraries]中
jimmer-ksp-jdbc2entity = { module = "org.babyfish.jimmer:jimmer-ksp-jdbc2entity", version .ref = "jimmer" }

# 在backend:model模块的dependencies {}中加入:
    ksp(libs.jimmer.ksp.jdbc2entity)
以及上述提到的ksp配置(想在buildSrc中配置的,参考jimmer的buidSrc目录,把
[ksp4jdbc.gradle.kts](../buildSrc/src/main/kotlin/org/babyfish/ksp/ksp4jdbc.gradle.kts)
[ksp4projectdir.gradle.kts](../buildSrc/src/main/kotlin/org/babyfish/ksp/ksp4projectdir.gradle.kts)
以及获取spring上下文jdbc配置的逻辑抄过去即可,使用前请看kts里的注释!
也可以写静态的配置
)

