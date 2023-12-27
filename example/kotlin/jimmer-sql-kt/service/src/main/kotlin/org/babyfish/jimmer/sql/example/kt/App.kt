package org.babyfish.jimmer.sql.example.kt

import org.babyfish.jimmer.client.EnableImplicitApi
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableImplicitApi
class App

fun main(args: Array<String>) {
	runApplication<App>(*args)
}
