package com.example.webscrapper.repository

import com.example.webscrapper.model.Task
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface TaskRepository : JpaRepository<Task, Long> {

    @Query("SELECT t FROM Task t WHERE t.enabled = true")
    fun findAllEnabledTasks(): List<Task>

    @Query("SELECT t FROM Task t WHERE t.url = :url AND t.xpath = :xpath")
    fun findTaskByUrlAndXpath(url: String, xpath: String): Task?
}