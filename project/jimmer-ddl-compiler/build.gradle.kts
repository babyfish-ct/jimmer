plugins {
    `kotlin-convention`
    `dokka-convention`
}

dependencies {
    implementation(libs.addzero.lsi.core)
    implementation(libs.addzero.lsi.ksp)
    implementation(libs.addzero.lsi.apt)
    implementation(libs.addzero.ddlgenerator.core)
    implementation(libs.addzero.ddlgenerator.lsi.adaptor)
    implementation(libs.addzero.ddlgenerator.dialect.mysql)
    implementation(libs.addzero.ddlgenerator.dialect.postgresql)
    implementation(libs.addzero.ddlgenerator.dialect.h2)
    implementation(libs.addzero.ddlgenerator.dialect.sqlite)
    implementation(libs.addzero.ddlgenerator.dialect.sqlserver)
    implementation(libs.addzero.ddlgenerator.dialect.oracle)
    implementation(libs.addzero.ddlgenerator.dialect.dm)
    implementation(libs.addzero.ddlgenerator.dialect.kingbase)
    implementation(libs.addzero.ddlgenerator.dialect.taos)
    implementation(libs.addzero.tool.database.model)
    implementation(libs.ksp.symbolProcessing.api)

    testImplementation(libs.kotlin.test)
}

tasks.test {
    useJUnit()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}
