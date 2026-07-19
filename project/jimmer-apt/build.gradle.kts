plugins {
    `java-convention`
}

dependencies {
    implementation(projects.jimmerMapstructApt)
    implementation(projects.jimmerCore)
    implementation(projects.jimmerDtoCompiler)

    implementation(libs.spring.core)
    implementation(libs.intellij.annotations)
    implementation(libs.javapoet)
    implementation(libs.jackson2.databind)
}
