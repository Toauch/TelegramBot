package com.schedule

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ScheduleBotApplication

fun main(args: Array<String>) {
    runApplication<ScheduleBotApplication>(*args)
} 