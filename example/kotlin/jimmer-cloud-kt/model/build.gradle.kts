plugins {
    kotlin("jvm") version "1.6.21"
    id("com.google.devtools.ksp") version "1.7.10-1.0.6"
}

repositories {
    mavenCentral()
}

val jimmerVersion: String by rootProject.extra

dependencies {

    implementation(kotlin("stdlib"))

    // For user code
    implementation("org.babyfish.jimmer:jimmer-core:${jimmerVersion}")

    // For generated code
    compileOnly("org.babyfish.jimmer:jimmer-sql-kotlin:${jimmerVersion}")

    // Code generator
    ksp("org.babyfish.jimmer:jimmer-ksp:${jimmerVersion}")
}

// Without this configuration, gradle command can still run.
// However, Intellij cannot find the generated source.
kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}