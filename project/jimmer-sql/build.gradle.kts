import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-convention`
    antlr
}

dependencies {
    api(projects.jimmerCore)
    implementation(libs.slf4j.api)
    implementation(libs.kotlin.stdlib)
    implementation(libs.jetbrains.annotations)
    implementation(libs.apache.commons.lang3)
    implementation(libs.jackson.datatype.jsr310)
    compileOnly(libs.postgresql)
    compileOnly(libs.jackson.module.kotlin)
    antlr(libs.antlr)

    testAnnotationProcessor(projects.jimmerApt)

    testImplementation(libs.jupiter.api)
    testRuntimeOnly(libs.jupiter.engine)

    testImplementation(libs.spring.jdbc)

    testImplementation(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    testImplementation(libs.h2)
    testImplementation(libs.mysql.connector.java)
    testImplementation(libs.postgresql)
    testImplementation(libs.kafka.connect.api)
    // testImplementation(files("/Users/chentao/Downloads/ojdbc8-21.9.0.0.jar"))
}

tasks.withType<Jar> {
    dependsOn(tasks.withType<AntlrTask>())
}

tasks.withType<KotlinCompile> {
    dependsOn(tasks.withType<AntlrTask>())
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xmaxerrs")
    options.compilerArgs.add("2000")
}
