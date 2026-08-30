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
}

tasks.test {
    useJUnit()

    // The intellij-core bundled in KSP2 hard-codes a feature version ceiling in
    // JavaVersion.parse; anything above it throws IllegalArgumentException while
    // CoreJrtFileSystem initializes. The ceiling moves up with intellij-core, so
    // this pins a version that stays inside the current window.
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
