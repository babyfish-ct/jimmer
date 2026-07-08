import org.gradle.jvm.tasks.Jar
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
    compileOnly(libs.jackson2.databind)
    compileOnly(libs.jackson3.databind)
    compileOnly(libs.h2)
    compileOnly(libs.postgresql)
    compileOnly(libs.caffeine)
    compileOnly(libs.spring.data.redis)
    compileOnly(libs.quarkus.redis.client)
    compileOnly(libs.redisson)
    compileOnly(libs.antlr) {
        exclude("com.ibm.icu", "icu4j")
    }
    antlr(libs.antlr) {
        exclude("com.ibm.icu", "icu4j")
    }

    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
    testAnnotationProcessor(projects.jimmerApt)

    testImplementation(libs.bundles.jackson)
    testImplementation(libs.spring.jdbc)
    testImplementation(libs.h2)
    testImplementation(libs.mysql.connector.java)
    testImplementation(libs.postgresql)
    testImplementation(libs.sqlite)
    testImplementation(libs.kafka.connect.api)
    testImplementation(libs.javax.validation.api)
    testImplementation(libs.hibernate.validation)
    testImplementation(libs.antlr)
    // testImplementation(files("/Users/chentao/Downloads/ojdbc8-21.9.0.0.jar"))
}

configurations.api {
    setExtendsFrom(extendsFrom.filter { it.name != AntlrPlugin.ANTLR_CONFIGURATION_NAME })
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
    options.compilerArgs.add("-Ajimmer.dto.fieldVisibility=protected")
    options.compilerArgs.add("-Ajimmer.jackson3=true")
}
