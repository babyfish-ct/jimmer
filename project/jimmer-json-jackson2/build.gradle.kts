plugins {
    `java-publish-convention`
}

dependencies {
    api(projects.jimmerCore)
    api(libs.jackson2.databind)
    implementation(libs.jackson2.module.kotlin)
    implementation(libs.jackson2.datatype.jsr310)
}
