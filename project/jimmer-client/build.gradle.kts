plugins {
    `kotlin-convention`
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(projects.jimmerSql)
    compileOnly(libs.spring.boot)
    annotationProcessor(libs.spring.boot.configurationProcessor)

    testAnnotationProcessor(projects.jimmerApt)
    kspTest(projects.jimmerKsp)
    testImplementation(projects.jimmerSqlKotlin)
    testImplementation(libs.jupiter.api)
    testRuntimeOnly(libs.jupiter.engine)
    testImplementation(libs.spring.web)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Ajimmer.source.includes=org.babyfish.jimmer.client.java.")
    options.compilerArgs.add("-Ajimmer.client.checkedException=true")
}

//tasks.compileTestJava {
//    options.release.set(21)
//}

ksp {
    arg("jimmer.source.includes", "org.babyfish.jimmer.client.kotlin.")
    arg("jimmer.dto.testDirs", "src/test/dto2")
}

kotlin {
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}
