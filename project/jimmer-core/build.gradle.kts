plugins {
    `kotlin-publish-convention`
    alias(libs.plugins.buildconfig)
}

dependencies {
    api(libs.jspecify)
    api(libs.kotlin.reflect)
    implementation(libs.javax.validation.api)
    implementation(libs.kotlin.stdlib)

    testImplementation(projects.jimmerJsonJackson2)
    testImplementation(projects.jimmerJsonJackson3)
    testImplementation(projects.jimmerJsonGson)
    testImplementation(projects.jimmerJsonFastjson2)
    testImplementation(libs.jackson2.datatype.jsr310)
    testImplementation(libs.mapstruct)
    testImplementation(libs.javax.validation.api)
    testCompileOnly(libs.lombok)
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
    packageName("org.babyfish.jimmer")
    className("JimmerVersion")
    buildConfigField("int", "major", versionParts[0])
    buildConfigField("int", "minor", versionParts[1])
    buildConfigField("int", "patch", versionParts[2])
    useKotlinOutput {
        internalVisibility = false
    }
}
