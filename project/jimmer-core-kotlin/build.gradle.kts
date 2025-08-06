plugins {
    `kotlin-convention`
    `dokka-convention`
    `ksp-convention`
}

dependencies {
    api(projects.jimmerCore)
    implementation(libs.kotlin.stdlib)

    testAnnotationProcessor(projects.jimmerApt)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mapstruct)
    testImplementation(libs.javax.validation.api)
    kspTest(projects.jimmerKsp)
}

kotlin {
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}

ksp {
    arg("jimmer.source.excludes", "org.babyfish.jimmer.kt.model.JavaData")
}

tasks.test {
    useJUnit()
}
