plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

val asm: String by project

dependencies {
    implementation(gradleApi())

    implementation("org.ow2.asm:asm:${asm}")
    implementation("org.ow2.asm:asm-analysis:${asm}")
    implementation("org.ow2.asm:asm-commons:${asm}")
    implementation("org.ow2.asm:asm-tree:${asm}")
    implementation("org.ow2.asm:asm-util:${asm}")
}