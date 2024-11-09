plugins {
    `kotlin-convention`
    alias(libs.plugins.buildconfig)
}

dependencies {
    implementation(libs.javax.validation.api)
    api(libs.jackson.databind)
    api(libs.kotlin.reflect)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.apache.commons.lang3)
    implementation(libs.kotlin.stdlib)
    compileOnly(libs.mapstruct)

    testImplementation(libs.jupiter.api)
    testImplementation(libs.mapstruct)
    testImplementation(libs.lombok)
    testRuntimeOnly(libs.jupiter.engine)

    testAnnotationProcessor(projects.jimmerApt)
    testAnnotationProcessor(libs.lombok)
    testAnnotationProcessor(libs.mapstruct.processor)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Ajimmer.source.excludes=org.babyfish.jimmer.invalid")
    options.compilerArgs.add("-Ajimmer.generate.dynamic.pojo=true")
}

buildConfig {
    val versionParts = (project.version as String).split('.')
    packageName(project.group as String)
    className("JimmerVersion")
    buildConfigField("int", "major", versionParts[0])
    buildConfigField("int", "minor", versionParts[1])
    buildConfigField("int", "patch", versionParts[2])
    useKotlinOutput {
        internalVisibility = false
    }
}