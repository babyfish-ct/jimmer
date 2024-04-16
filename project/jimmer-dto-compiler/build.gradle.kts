import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-convention`
    antlr
}

dependencies {
    implementation(libs.jetbrains.annotations)
    testImplementation(libs.jupiter.api)
    testRuntimeOnly(libs.jupiter.engine)
    antlr(libs.antlr)
}

tasks.withType<Jar>().configureEach {
    dependsOn(tasks.withType<AntlrTask>())
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(tasks.withType<AntlrTask>())
}