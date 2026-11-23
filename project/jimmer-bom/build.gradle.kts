plugins {
    `java-platform`
    `publish-convention`
}

dependencies {
    constraints {
        api(projects.jimmerApt)
        api(projects.jimmerClient)
        api(projects.jimmerClientSwagger)
        api(projects.jimmerClientScalar)
        api(projects.jimmerCore)
        api(projects.jimmerJsonJackson2)
        api(projects.jimmerJsonJackson3)
        api(projects.jimmerJsonGson)
        api(projects.jimmerJsonFastjson2)
        api(projects.jimmerCoreKotlin)
        api(projects.jimmerDtoCompiler)
        api(projects.jimmerDdlCompiler)
        api(projects.jimmerKsp)
        api(projects.jimmerMapstructApt)
        api(projects.jimmerSpringBootStarter)
        api(projects.jimmerSql)
        api(projects.jimmerSqlKotlin)
    }
}
