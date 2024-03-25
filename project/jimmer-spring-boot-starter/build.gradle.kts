plugins {
    `kotlin-convention`
    alias(libs.plugins.ksp)
}

dependencies {
    api(projects.jimmerSql)
    api(projects.jimmerSqlKotlin)
    api(projects.jimmerClient)
    api(libs.spring.boot.starter.jdbc)
    api(libs.spring.data.commons)
    api(libs.jackson.module.kotlin)

    compileOnly(libs.spring.boot.starter.web)
    compileOnly(libs.spring.data.redis)
    compileOnly(libs.caffeine)
    compileOnly(libs.spring.graphql)
    compileOnly(libs.jakartaee.api)
    compileOnly(libs.springdoc.openapi.common)

    annotationProcessor(libs.spring.boot.configurationProcessor)
    testAnnotationProcessor(projects.jimmerApt)
    kspTest(projects.jimmerKsp)

    testImplementation(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.jupiter.api)
    testImplementation(libs.h2)
    testRuntimeOnly(libs.jupiter.engine)
    testImplementation(libs.spring.boot.starter.web)
}

kotlin {
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}

ksp {
    arg("jimmer.source.excludes", "org.babyfish.jimmer.spring.java")
}