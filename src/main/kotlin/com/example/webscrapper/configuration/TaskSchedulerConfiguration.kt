package com.example.webscrapper.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

@Configuration
class TaskSchedulerConfiguration {

    @Value("\${web-scrapper.thread-pool-size}")
    var threadPoolSize: Int = 16

    @Bean
    fun scheduledExecutorService(): ScheduledExecutorService =
        Executors.newScheduledThreadPool(threadPoolSize)
}