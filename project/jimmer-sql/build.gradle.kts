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
    compileOnly(libs.caffeine)
    compileOnly(libs.spring.data.redis)
    compileOnly(libs.quarkus.redis.client)
    compileOnly(libs.redisson)
    antlr(libs.antlr) {
        exclude("com.ibm.icu", "icu4j")
    }

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
    testImplementation(libs.javax.validation.api)
    testImplementation(libs.hibernate.validation)
    // testImplementation(files("/Users/chentao/Downloads/ojdbc8-21.9.0.0.jar"))
}

tasks.withType<Jar>().configureEach {
    dependsOn(tasks.withType<AntlrTask>())
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(tasks.withType<AntlrTask>())
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xmaxerrs")
    options.compilerArgs.add("2000")
    options.compilerArgs.add("-Ajimmer.dto.hibernateValidatorEnhancement=true")
}
