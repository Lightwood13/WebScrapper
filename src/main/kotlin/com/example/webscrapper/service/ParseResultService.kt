package com.example.webscrapper.service

import com.example.webscrapper.model.ParseResult
import com.example.webscrapper.repository.ParseResultRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
@Transactional
class ParseResultService(
    private val parseResultRepository: ParseResultRepository
) {
    fun getParseResults(page: Int, pageSize: Int): Page<ParseResult> =
        parseResultRepository.findAll(
            PageRequest.of(page, pageSize, Sort.by("parsedOn").descending())
        )

    fun save(parseResult: ParseResult): ParseResult = parseResultRepository.save(parseResult)

    fun detachFromTask(taskId: Long): Unit = parseResultRepository.detachFromTask(taskId)
}