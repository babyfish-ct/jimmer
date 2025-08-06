plugins {
    `kotlin-convention`
    `dokka-convention`
    `ksp-convention`
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    api(projects.jimmerCoreKotlin)
    api(projects.jimmerSql)
    implementation(libs.apache.commons.lang3)

    testImplementation(libs.kotlin.test)
    kspTest(projects.jimmerKsp)
    testAnnotationProcessor(projects.jimmerKsp)

    testImplementation(libs.h2)
    testImplementation(libs.mysql.connector.java)
    testImplementation(libs.postgresql)
    testImplementation(libs.javax.validation.api)
    testImplementation(libs.jackson.datatype.jsr310)
    testImplementation(libs.spring.jdbc)
    testImplementation(libs.jackson.module.kotlin)
    testImplementation(libs.hibernate.validation)
    testImplementation(libs.caffeine)
}

ksp {
    arg("jimmer.dto.mutable", "true")
    arg("jimmer.dto.hibernateValidatorEnhancement", "true")
}

kotlin {
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}

tasks.test {
    useJUnit()
}
