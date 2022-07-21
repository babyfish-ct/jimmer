plugins {
    kotlin("jvm") version "1.6.10"
    id("com.google.devtools.ksp") version "1.6.21-1.0.5"
    id("org.jetbrains.dokka") version "1.6.10"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    api(project(":jimmer-core-kotlin"))
    api(project(":jimmer-sql"))
    dokkaHtmlPlugin("org.jetbrains.dokka:dokka-base:1.6.20")

    testImplementation(kotlin("test"))
    kspTest(project(":jimmer-ksp"))
}

kotlin {
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}