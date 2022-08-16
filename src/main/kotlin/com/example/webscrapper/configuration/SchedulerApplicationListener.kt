package com.example.webscrapper.configuration

import com.example.webscrapper.service.SchedulerService
import com.example.webscrapper.service.TaskService
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class SchedulerApplicationListener(
    private val schedulerService: SchedulerService,
    private val taskService: TaskService
) {

    @EventListener(classes = [ContextRefreshedEvent::class])
    fun onRefreshedEvent(event: ContextRefreshedEvent) =
        schedulerService.scheduleTasks(taskService.getEnabledTasks())

    @EventListener(classes = [ContextClosedEvent::class])
    fun onClosedEvent(event: ContextClosedEvent) =
        schedulerService.cancelAllTasks()
}