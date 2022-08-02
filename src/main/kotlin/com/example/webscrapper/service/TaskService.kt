package com.example.webscrapper.service

import com.example.webscrapper.controller.dto.TaskUpdateDTO
import com.example.webscrapper.exception.NotFoundException
import com.example.webscrapper.model.Task
import com.example.webscrapper.repository.TaskRepository
import org.apache.commons.validator.routines.UrlValidator
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionalEventListener
import us.codecraft.xsoup.Xsoup
import javax.transaction.Transactional

@Service
@Transactional
class TaskService(
    private val taskRepository: TaskRepository,
    private val parseResultService: ParseResultService,
    private val schedulerService: SchedulerService,
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    private val urlValidator = UrlValidator(arrayOf("http", "https"))

    fun getTasks(): List<Task> = taskRepository.findAll()

    fun getEnabledTasks(): List<Task> = taskRepository.findAllEnabledTasks()

    fun throwIfExists(url: String, xpath: String) {
        if (taskRepository.findTaskByUrlAndXpath(url, xpath) != null)
            throw IllegalArgumentException("Task with such url:xpath already exists")
    }

    fun createTask(task: Task): Task = task.let(this::validateTask)
        .also { throwIfExists(task.url, task.xpath) }
        .let(taskRepository::save)
        .also { applicationEventPublisher.publishEvent(TaskCreatedEvent(it)) }

    fun deleteTask(taskId: Long) {
        parseResultService.detachFromTask(taskId)
        taskRepository.deleteById(taskId)
        applicationEventPublisher.publishEvent(TaskDeletedEvent(taskId))
    }

    fun updateTask(taskId: Long, taskUpdateDTO: TaskUpdateDTO): Task {
        val task = taskRepository.findByIdOrNull(taskId)
            ?: throw NotFoundException()

        if (taskUpdateDTO.url != null || taskUpdateDTO.xpath != null) {
            throwIfExists(
                taskUpdateDTO.url ?: task.url,
                taskUpdateDTO.xpath ?: task.xpath
            )
        }

        taskUpdateDTO.url?.let { task.url = validateUrl(it) }
        taskUpdateDTO.xpath?.let { task.xpath = validateXpath(it) }
        taskUpdateDTO.enabled?.let { task.enabled = it }
        taskUpdateDTO.intervalMillis?.let { task.intervalMillis = it }

        applicationEventPublisher.publishEvent(TaskUpdatedEvent(task))
        return task
    }

    @TransactionalEventListener
    fun handleTaskCreatedEvent(event: TaskCreatedEvent) {
        if (event.task.enabled)
            schedulerService.scheduleTask(event.task)
    }

    @TransactionalEventListener
    fun handleTaskUpdatedEvent(event: TaskUpdatedEvent) {
        if (event.task.enabled)
            schedulerService.scheduleTask(event.task)
        else
            schedulerService.cancelTask(event.task.id!!)
    }

    @TransactionalEventListener
    fun handleTaskDeletedEvent(event: TaskDeletedEvent) {
        schedulerService.cancelTask(event.taskId)
    }

    private fun validateTask(task: Task): Task = task.also {
        validateUrl(task.url)
        validateXpath(task.xpath)
    }

    private fun validateUrl(url: String): String = url.also {
        if (!urlValidator.isValid(url))
            throw IllegalArgumentException("Invalid url")
    }

    private fun validateXpath(xpath: String): String = xpath.also {
        try {
            Xsoup.compile(xpath)
        } catch (_: Exception) {
            throw IllegalArgumentException("Invalid xpath")
        }
    }
}

data class TaskCreatedEvent(val task: Task)
data class TaskUpdatedEvent(val task: Task)
data class TaskDeletedEvent(val taskId: Long)