package org.babyfish.jimmer.example.cloud.kt.book

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BookServiceApp

fun main(args: Array<String>) {
    runApplication<BookServiceApp>(*args)
}