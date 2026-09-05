plugins {
    `kotlin-publish-convention`
    `dokka-convention`
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(projects.jimmerCore)
    implementation(projects.jimmerDtoCompiler)
    implementation(libs.ksp.symbolProcessing.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.jackson2.databind)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.dev.zacsweers.kctfork.ksp) {
        exclude(module = "symbol-processing-api")
    }

    testImplementation(projects.jimmerSqlKotlin)
    testImplementation(libs.javax.validation.api)
    testImplementation(libs.hibernate.validation)
}

tasks.test {
    useJUnit()
}
