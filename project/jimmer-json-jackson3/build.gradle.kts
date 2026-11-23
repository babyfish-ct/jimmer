plugins {
    `java-publish-convention`
}

dependencies {
    api(projects.jimmerCore)
    api(libs.jackson3.databind)
    implementation(libs.jackson3.module.kotlin)
}
