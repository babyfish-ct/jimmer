package org.babyfish.jimmer.example.cloud.kt.author

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AuthorServiceApp

fun main(args: Array<String>) {
    runApplication<AuthorServiceApp>(*args)
}