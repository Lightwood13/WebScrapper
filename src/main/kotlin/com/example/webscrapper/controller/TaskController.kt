package com.example.webscrapper.controller

import com.example.webscrapper.controller.dto.TaskUpdateDTO
import com.example.webscrapper.model.Task
import com.example.webscrapper.service.TaskService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/tasks")
class TaskController(
    private val taskService: TaskService
) {

    @GetMapping
    fun getTasks(): List<Task> = taskService.getTasks()

    @PostMapping
    fun createTask(@RequestBody task: Task): Task = taskService.createTask(task)

    @DeleteMapping("/{taskId}")
    fun deleteTask(@PathVariable taskId: Long): Unit = taskService.deleteTask(taskId)

    @PatchMapping("/{taskId}")
    fun updateTask(
        @PathVariable taskId: Long,
        @RequestBody taskUpdateDTO: TaskUpdateDTO
    ): Task = taskService.updateTask(taskId, taskUpdateDTO)
}