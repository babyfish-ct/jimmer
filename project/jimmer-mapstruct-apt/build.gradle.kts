plugins {
    `java-convention`
}

dependencies {
    compileOnly(libs.mapstruct.processor)
    implementation(projects.jimmerCore)
    testImplementation(libs.jupiter.api)
    testRuntimeOnly(libs.jupiter.engine)
}
