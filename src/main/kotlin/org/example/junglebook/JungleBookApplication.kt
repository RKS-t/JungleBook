package org.example.junglebook

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class JungleBookApplication

fun main(args: Array<String>) {
    runApplication<JungleBookApplication>(*args)
}
