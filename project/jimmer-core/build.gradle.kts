plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    api(libs.javax.validationApi)
    api(libs.jackson.databind)
    api(libs.kotlin.reflect)
    implementation(libs.jackson.datatype.jsr310)
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

tasks.withType(JavaCompile::class) {
    options.compilerArgs.add("-parameters")
    options.compilerArgs.add("-Ajimmer.source.excludes=org.babyfish.jimmer.invalid")
    options.compilerArgs.add("-Ajimmer.generate.dynamic.pojo=true")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<Javadoc>{
    options.encoding = "UTF-8"
}