package com.example.webscrapper.controller

import com.example.webscrapper.model.ParseResult
import com.example.webscrapper.service.ParseResultService
import org.springframework.data.domain.Page
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/parse-results")
class ParseResultController(
    private val parseResultService: ParseResultService
) {

    @GetMapping
    fun getParseResults(
        @RequestParam("page") page: Int,
        @RequestParam("page_size") pageSize: Int
    ): Page<ParseResult> = parseResultService.getParseResults(page, pageSize)
}