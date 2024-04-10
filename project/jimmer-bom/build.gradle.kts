plugins {
    `java-platform`
    `publish-convention`
}

dependencies {
    constraints {
        api(projects.jimmerApt)
        api(projects.jimmerClient)
        api(projects.jimmerClientSwagger)
        api(projects.jimmerCore)
        api(projects.jimmerCoreKotlin)
        api(projects.jimmerDtoCompiler)
        api(projects.jimmerKsp)
        api(projects.jimmerMapstructApt)
        api(projects.jimmerSpringBootStarter)
        api(projects.jimmerSql)
        api(projects.jimmerSqlKotlin)
    }
}
