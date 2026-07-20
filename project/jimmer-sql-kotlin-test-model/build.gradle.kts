plugins {
    `kotlin-convention`
    alias(libs.plugins.ksp)
}

dependencies {
    api(projects.jimmerSqlKotlin)

    compileOnly(libs.bundles.jackson)
    compileOnly(libs.hibernate.validation)
    compileOnly(libs.javax.validation.api)
    compileOnly(libs.postgresql)
    compileOnly(libs.easyexcel)

    ksp(projects.jimmerKsp)
    ksp(files("src/main/dto-bundle"))
}

ksp {
    arg("jimmer.dto.mutable", "true")
    arg("jimmer.dto.hibernateValidatorEnhancement", "true")
//    arg("jimmer.jackson3", "true")
}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}
