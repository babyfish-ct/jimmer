plugins {
    `kotlin-convention`
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
}
