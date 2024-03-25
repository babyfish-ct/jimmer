plugins {
    `kotlin-convention`
    `dokka-convention`
    alias(libs.plugins.ksp)
}

dependencies {
    api(projects.jimmerCore)
    implementation(libs.kotlin.stdlib)

    testAnnotationProcessor(projects.jimmerApt)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mapstruct)
    kspTest(projects.jimmerKsp)

    dokkaHtmlPlugin(libs.dokka.base)
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