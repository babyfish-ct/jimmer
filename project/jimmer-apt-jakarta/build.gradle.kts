import cn.enaium.jimmer.JakartaTransformExtension

plugins {
    `java-library`
    id("jakarta-transform")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
}

configure<JakartaTransformExtension> {
    mirror = project(":jimmer-apt")
}