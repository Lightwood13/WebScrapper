package com.example.webscrapper.repository

import com.example.webscrapper.model.ParseResult
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface ParseResultRepository : JpaRepository<ParseResult, Long> {

    @Modifying
    @Query("UPDATE ParseResult pr SET pr.task = null WHERE pr.task.id = :taskId")
    fun detachFromTask(taskId: Long)

    @EntityGraph(attributePaths = ["task"])
    override fun findAll(pageable: Pageable): Page<ParseResult>
}