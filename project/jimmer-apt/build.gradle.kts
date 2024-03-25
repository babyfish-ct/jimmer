plugins {
    `java-convention`
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
