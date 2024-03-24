import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    antlr
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
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
//    testImplementation("com.fasterxml.uuid:java-uuid-generator:4.0.1")

    testImplementation(libs.lombok)
    testAnnotationProcessor(libs.lombok)

    testImplementation(libs.h2)
    testImplementation(libs.mysql.connector.java)
    testImplementation(libs.postgresql)
    testImplementation(libs.kafka.connect.api)
    // testImplementation(files("/Users/chentao/Downloads/ojdbc8-21.9.0.0.jar"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<Javadoc>{
    options.encoding = "UTF-8"
}

tasks.withType<Jar>().configureEach {
    dependsOn(tasks.withType<AntlrTask>())
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(tasks.withType<AntlrTask>())
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
    options.compilerArgs.add("-Xmaxerrs")
    options.compilerArgs.add("2000")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=all")
        jvmTarget = "1.8"
    }
}