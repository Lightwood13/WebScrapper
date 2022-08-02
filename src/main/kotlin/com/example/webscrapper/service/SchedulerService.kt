package com.example.webscrapper.service

import com.example.webscrapper.model.Task
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Service
class SchedulerService(
    private val scheduledExecutorService: ScheduledExecutorService,
    private val scrapperTaskFactory: ScrapperTaskFactory
) {

    private val runningTasks: MutableMap<Long, ScheduledFuture<*>> =
        ConcurrentHashMap<Long, ScheduledFuture<*>>()

    fun scheduleTasks(tasks: List<Task>): Unit = tasks.forEach(this::scheduleTask)

    fun scheduleTask(task: Task) {
        runningTasks.compute(task.id!!) { _, oldFuture ->
            oldFuture?.cancel(true)

            scheduledExecutorService.scheduleAtFixedRate(
                scrapperTaskFactory.createScrapperTask(task),
                0,
                task.intervalMillis,
                TimeUnit.MILLISECONDS
            )
        }
    }

    fun cancelTask(taskId: Long): Boolean =
        runningTasks.remove(taskId)?.cancel(true) ?: false

    fun cancelAllTasks(): Unit = runningTasks.keys.forEach(this::cancelTask)
}