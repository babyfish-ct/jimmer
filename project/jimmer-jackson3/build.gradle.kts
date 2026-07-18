plugins {
    `java-convention`
    `dokka-convention`
}

dependencies {
    api(projects.jimmerCore)
    api(libs.jackson3.databind)
    implementation(libs.jackson.annotations)
    implementation(libs.jackson3.module.kotlin)
}
