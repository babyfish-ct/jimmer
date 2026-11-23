plugins {
    `kotlin-publish-convention`
    `ksp-convention`
}

dependencies {
    implementation(projects.jimmerSql)
    compileOnly(libs.spring.boot)
    annotationProcessor(libs.spring.boot.configurationProcessor)

    testAnnotationProcessor(projects.jimmerApt)
    kspTest(projects.jimmerKsp)

    testImplementation(projects.jimmerSqlKotlin)
    testImplementation(libs.spring.web)
    testImplementation(libs.bundles.jackson)
    testImplementation(projects.jimmerJsonJackson2)
    testImplementation(projects.jimmerJsonJackson3)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Ajimmer.source.includes=org.example.unused, org.babyfish.jimmer.client.java.")
    options.compilerArgs.add(
        "-Ajimmer.source.excludes=org.example.internal; " +
            "org.babyfish.jimmer.client.java.model.ExcludedDefaultTarget"
    )
    options.compilerArgs.add("-Ajimmer.client.checkedException=true")
}

//tasks.compileTestJava {
//    options.release.set(21)
//}

ksp {
    arg("jimmer.source.includes", "org.example.unused, org.babyfish.jimmer.client.kotlin.")
    arg(
        "jimmer.source.excludes",
        "org.example.internal; org.babyfish.jimmer.client.kotlin.model.KExcludedDefaultTarget"
    )
}

dtoDirectories {
    test.set(listOf("src/test/dto2"))
}

kotlin {
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}
