plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.gradlePlugin.ksp)
    implementation("org.yaml:snakeyaml:2.2")
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20")
    api("org.jetbrains.dokka:dokka-gradle-plugin:2.0.0")
    api("com.vanniktech:gradle-maven-publish-plugin:0.33.0")
}
