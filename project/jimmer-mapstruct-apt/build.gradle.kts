plugins {
    `java-publish-convention`
}

dependencies {
    compileOnly(libs.mapstruct.processor)
    implementation(projects.jimmerCore)
}
