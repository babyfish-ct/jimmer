plugins {
    `kotlin-convention`
    `dokka-convention`
    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    api(projects.jimmerCore)
    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)

    testImplementation(libs.kotlin.test)
}
