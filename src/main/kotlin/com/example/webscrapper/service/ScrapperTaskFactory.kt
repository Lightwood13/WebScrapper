package com.example.webscrapper.service

import com.example.webscrapper.model.ParseResult
import com.example.webscrapper.model.Task
import org.apache.logging.log4j.LogManager
import org.jsoup.Jsoup
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import us.codecraft.xsoup.Xsoup


@Service
class ScrapperTaskFactory(
    private val webClient: WebClient,
    private val parseResultService: ParseResultService
) {
    private val logger = LogManager.getLogger(ScrapperTaskFactory::class.java)

    fun createScrapperTask(task: Task) = Runnable {
        webClient
            .get()
            .uri(task.url)
            .retrieve()
            .bodyToMono(String::class.java)
            .subscribe(
                { pageContent -> parseWebPage(pageContent, task) },
                { err -> logger.error(err) }
            )
    }

    private fun parseWebPage(pageContent: String, task: Task) {
        Xsoup
            .compile(task.xpath)
            .evaluate(Jsoup.parse(pageContent))
            ?.toString()
            .let { ParseResult(task, it ?: "") }
            .let(parseResultService::save)
    }
}