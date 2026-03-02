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

    compileOnly(libs.jackson2.databind)
    compileOnly(libs.jackson3.databind)
    compileOnly(libs.spring.boot.starter.web)
    compileOnly(libs.spring.data.redis)
    compileOnly(libs.caffeine)
    compileOnly(libs.spring.graphql)
    compileOnly(libs.jakartaee.api)
    compileOnly(libs.springdoc.openapi.common)

    annotationProcessor(libs.spring.boot.configurationProcessor)

    testAnnotationProcessor(projects.jimmerApt)
    testAnnotationProcessor(libs.lombok)

    kspTest(projects.jimmerKsp)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.web)
    testImplementation(libs.h2)
    testRuntimeOnly(libs.bundles.jackson)
    testRuntimeOnly(projects.jimmerClientSwagger)
}

tasks.processResources {
    inputs.property("swagger", libs.versions.swaggerUi.get())
    filesMatching("application.properties") {
        expand(
            mapOf(
                "swaggerUiVersion" to libs.versions.swaggerUi.get().toString()
            )
        )
    }
}

kotlin {
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}

ksp {
    arg("jimmer.source.excludes", "org.babyfish.jimmer.spring.java")
}