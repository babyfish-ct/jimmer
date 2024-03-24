plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.dokka)
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

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks {
    withType(Jar::class) {
        if (archiveClassifier.get() == "javadoc") {
            dependsOn(dokkaHtml)
            from("build/dokka/html")
        }
    }
}

tasks.withType<Javadoc>{
    options.encoding = "UTF-8"
}
