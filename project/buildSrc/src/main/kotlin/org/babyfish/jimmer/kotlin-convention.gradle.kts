import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-convention")
    kotlin("jvm")
}

val javaVersion = extensions.getByName<JavaPluginExtension>("java").targetCompatibility.toString()
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xjvm-default=all")
        jvmTarget = JvmTarget.fromTarget(javaVersion)
        javaParameters = true
    }
}