package org.babyfish.jimmer.example.kt.sql

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class JimmerSqlApplication

fun main(args: Array<String>) {
	runApplication<JimmerSqlApplication>(*args)
}
