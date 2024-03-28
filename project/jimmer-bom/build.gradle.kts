plugins {
    `java-platform`
    `publish-convention`
}

dependencies {
    constraints {
        api(platform(projects.jimmerApt))
        api(platform(projects.jimmerClient))
        api(platform(projects.jimmerClientSwagger))
        api(platform(projects.jimmerCore))
        api(platform(projects.jimmerCoreKotlin))
        api(platform(projects.jimmerDtoCompiler))
        api(platform(projects.jimmerKsp))
        api(platform(projects.jimmerMapstructApt))
        api(platform(projects.jimmerSpringBootStarter))
        api(platform(projects.jimmerSql))
        api(platform(projects.jimmerSqlKotlin))
    }
}
