plugins {
    `kotlin-convention`
    `dokka-convention`
}

dependencies {
    implementation(projects.jimmerLowQueryAnnotations)
    implementation(libs.ksp.symbolProcessing.api)

    testImplementation(libs.kotlin.test)
}

tasks.test {
    useJUnit()
}
