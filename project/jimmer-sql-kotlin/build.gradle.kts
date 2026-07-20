plugins {
    `kotlin-publish-convention`
    `dokka-convention`
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    api(projects.jimmerCoreKotlin)
    api(projects.jimmerSql)

    testImplementation(libs.kotlin.test)
    testImplementation(projects.jimmerSqlTestSupport)
    testImplementation(projects.jimmerSqlKotlinTestModel)

    testImplementation(libs.bundles.jackson)
    testImplementation(libs.h2)
    testImplementation(libs.mysql.connector.java)
    testImplementation(libs.postgresql)
    testImplementation(libs.javax.validation.api)
    testImplementation(libs.spring.jdbc)
    testImplementation(libs.hibernate.validation)
    testImplementation(libs.caffeine)
}

tasks.test {
    useJUnit()
}
