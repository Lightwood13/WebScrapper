package com.example.webscrapper.integration

import com.example.webscrapper.model.ParseResult
import com.example.webscrapper.model.Task
import com.example.webscrapper.repository.ParseResultRepository
import com.example.webscrapper.repository.TaskRepository
import com.example.webscrapper.util.ResponsePage
import com.example.webscrapper.util.stringToObject
import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers


@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@TestPropertySource(locations = ["classpath:application-integrationtest.properties"])
class ParseResultIntegrationTest {

    @Autowired
    lateinit var taskRepository: TaskRepository

    @Autowired
    lateinit var parseResultRepository: ParseResultRepository

    @Autowired
    lateinit var mvc: MockMvc

    @BeforeEach
    fun setUp() {
        parseResultRepository.deleteAll()
        taskRepository.deleteAll()
    }

    @Test
    fun `get on parse-result endpoint should return parse results in descending order by date`() {
        val task = taskRepository.save(Task("https://google.com", "//div"))
        parseResultRepository.saveAll(
            listOf(
                ParseResult(task, "result1"),
                ParseResult(task, "result2"),
                ParseResult(task, "result3")
            )
        )

        val result = mvc.perform(
            MockMvcRequestBuilders.get("/parse-results")
                .queryParam("page", "0")
                .queryParam("page_size", "3")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andReturn()

        val receivedPage = stringToObject(
            result.response.contentAsString,
            object : TypeReference<ResponsePage<ParseResult>>() {}
        )

        assertThat(receivedPage.content)
            .extracting<String> { it.result }
            .containsExactly("result3", "result2", "result1")
    }

    @Test
    fun `get on parse-result endpoint should return parse results in pages`() {
        val task = taskRepository.save(Task("https://google.com", "//div"))
        parseResultRepository.saveAll(
            listOf(
                ParseResult(task, "result1"),
                ParseResult(task, "result2"),
                ParseResult(task, "result3")
            )
        )

        val page0Result = mvc.perform(
            MockMvcRequestBuilders.get("/parse-results")
                .queryParam("page", "0")
                .queryParam("page_size", "2")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andReturn()

        val page1Result = mvc.perform(
            MockMvcRequestBuilders.get("/parse-results")
                .queryParam("page", "1")
                .queryParam("page_size", "2")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andReturn()

        val receivedPage0 = stringToObject(
            page0Result.response.contentAsString,
            object : TypeReference<ResponsePage<ParseResult>>() {}
        )
        val receivedPage1 = stringToObject(
            page1Result.response.contentAsString,
            object : TypeReference<ResponsePage<ParseResult>>() {}
        )

        assertThat(receivedPage0.content)
            .extracting<String> { it.result }
            .containsExactly("result3", "result2")
        assertThat(receivedPage1.content)
            .extracting<String> { it.result }
            .containsExactly("result1")
    }
}