package org.babyfish.jimmer.example.cloud.kt.store

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class StoreServiceApp

fun main(args: Array<String>) {
    runApplication<StoreServiceApp>(*args)
}