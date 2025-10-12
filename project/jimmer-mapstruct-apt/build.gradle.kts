plugins {
    `java-convention`
}

dependencies {
    compileOnly(libs.mapstruct.processor)
    implementation(projects.jimmerCore)
}
