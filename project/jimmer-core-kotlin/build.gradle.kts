plugins {
    kotlin("jvm") version "1.6.21"
    id("com.google.devtools.ksp") version "1.6.21-1.0.5"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":jimmer-core"))
    implementation(kotlin("stdlib"))

    testAnnotationProcessor(project(":jimmer-apt"))
    testImplementation(kotlin("test"))

    kspTest(project(":jimmer-ksp"))
}

kotlin {
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}