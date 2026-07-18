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
        api(projects.jimmerCoreKotlin)
        api(projects.jimmerKotlinxSerialization)
        api(projects.jimmerJackson2)
        api(projects.jimmerJackson3)
        api(projects.jimmerDtoCompiler)
        api(projects.jimmerDdlCompiler)
        api(projects.jimmerKsp)
        api(projects.jimmerMapstructApt)
        api(projects.jimmerSpringBootStarter)
        api(projects.jimmerSpringBootStarterJackson)
        api(projects.jimmerSql)
        api(projects.jimmerSqlKotlin)
    }
}
