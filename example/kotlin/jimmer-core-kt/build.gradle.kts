plugins {
    kotlin("jvm") version "1.6.10"

    id("com.google.devtools.ksp") version "1.7.10-1.0.6"
}

val jimmerVersion = "0.6.58-0.7-preview"

group = "org.babyfish.jimmer.example.kt"
version = jimmerVersion

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation("org.babyfish.jimmer:jimmer-core-kotlin:${jimmerVersion}")
    ksp("org.babyfish.jimmer:jimmer-ksp:${jimmerVersion}")
}

// Without this configuration, gradle command can still run.
// However, Intellij cannot find the generated source.
kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}
