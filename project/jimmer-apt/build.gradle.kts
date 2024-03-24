plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    implementation(projects.jimmerMapstructApt)
    implementation(projects.jimmerCore)
    implementation(projects.jimmerDtoCompiler)

    implementation(libs.javax.validationApi)
    implementation(libs.spring.core)
    implementation(libs.intellij.annotations)
    implementation(libs.javapoet)

    testImplementation(libs.jupiter.api)
    testRuntimeOnly(libs.jupiter.engine)
}

tasks.withType(JavaCompile::class) {
    options.compilerArgs.add("-parameters")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<Javadoc>{
    options.encoding = "UTF-8"
}
