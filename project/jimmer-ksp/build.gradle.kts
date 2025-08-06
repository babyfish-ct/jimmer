plugins {
    `kotlin-convention`
    `dokka-convention`
    `ksp-convention`
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(projects.jimmerCore)
    implementation(projects.jimmerDtoCompiler)
    implementation(libs.ksp.symbolProcessing.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.javax.validation.api)
    implementation(libs.jakarta.validation.api)
}
