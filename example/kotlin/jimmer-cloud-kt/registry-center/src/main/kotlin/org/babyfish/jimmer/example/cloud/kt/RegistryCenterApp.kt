package org.babyfish.jimmer.example.cloud.kt

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer

@SpringBootApplication
@EnableEurekaServer
class RegistryCenterApp

fun main(args: Array<String>) {
    runApplication<RegistryCenterApp>(*args)
}