plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.ksp)
    alias(libs.plugins.springboot)
    alias(libs.plugins.spring.dependency.management)
}

group = "org.babyfish.jimmer"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(libs.jmh.core)
    annotationProcessor(libs.jmh.generator.annprocess)

    implementation(libs.jimmer.sql)
    annotationProcessor(libs.jimmer.apt)
    ksp(libs.jimmer.ksp)

    implementation(libs.spring.boot.jdbc)
    implementation(libs.spring.boot.data.jpa)
    implementation(libs.spring.boot.data.jdbc)
    implementation(libs.eclipselink)
    implementation(libs.jooq)
    implementation(libs.mybatis)
    implementation(libs.mybatis.plus)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.ktorm)
    implementation(libs.objsql)
    implementation(libs.nutz)
    implementation(libs.easyquery)
    implementation(libs.apijson)
    implementation(libs.apijson.framework)
    implementation(libs.mug.safesql)
    implementation(libs.sqltoy)
    implementation(libs.komapper.starter.jdbc)
    implementation(libs.komapper.dialect.h2.jdbc)
    ksp(libs.komapper.processor)

    runtimeOnly(libs.h2)
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Ajimmer.source.excludes=org.babyfish.jimmer.benchmark.jimmer.kt")
}

ksp {
    arg("jimmer.source.excludes", "org.babyfish.jimmer.benchmark.jimmer.JimmerData")
}

tasks.test {
    useJUnitPlatform()
}
