package com.example.webscrapper

import com.example.webscrapper.controller.dto.TaskUpdateDTO
import com.example.webscrapper.model.ParseResult
import com.example.webscrapper.model.Task
import com.example.webscrapper.repository.ParseResultRepository
import com.example.webscrapper.repository.TaskRepository
import com.example.webscrapper.service.SchedulerService
import com.example.webscrapper.service.TaskService
import com.example.webscrapper.util.objectToString
import com.example.webscrapper.util.stringToObject
import com.fasterxml.jackson.core.type.TypeReference
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.concurrent.TimeUnit


@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@TestPropertySource(locations = ["classpath:application-integrationtest.properties"])
class TaskIntegrationTest {

    @Autowired
    lateinit var taskService: TaskService

    @Autowired
    lateinit var taskRepository: TaskRepository

    @Autowired
    lateinit var parseResultRepository: ParseResultRepository

    @SpyBean
    lateinit var schedulerService: SchedulerService

    @Autowired
    lateinit var mvc: MockMvc

    lateinit var mockWebServer: MockWebServer

    lateinit var mockServerUrl: String

    @BeforeEach
    fun setUp() {
        parseResultRepository.deleteAll()
        taskRepository.deleteAll()
        schedulerService.cancelAllTasks()

        mockWebServer = MockWebServer()
        mockWebServer.start()
        mockServerUrl = "http://127.0.0.1:${mockWebServer.port}"
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `post on task endpoint should save task to database and schedule job`() {
        val task = objectToString(Task(mockServerUrl, "//div", true))

        val result = mvc.perform(
            post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(task)
        ).andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andReturn()

        val createdTask = stringToObject(result.response.contentAsString, Task::class)
        assertThat(createdTask.id).isNotNull
        val taskFromDatabase = taskRepository.findById(createdTask.id!!).get()
        assertThat(createdTask)
            .usingRecursiveComparison()
            .isEqualTo(taskFromDatabase)

        verify(schedulerService).scheduleTask(createdTask)

        val recordedRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS)
        assertThat(recordedRequest).isNotNull
            .extracting { it!!.method }
            .isEqualTo("GET")
    }

    @Test
    fun `post on task endpoint should validate task url`() {
        val task = objectToString(Task("invalid-url", "//div"))

        val response = mvc.perform(
            post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(task)
        ).andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andReturn()
            .response.contentAsString

        assertThat(response).isEqualTo("Invalid url")
    }

    @Test
    fun `post on task endpoint should validate task xpath`() {
        val task = objectToString(Task(mockServerUrl, "//div/text1()"))

        val response = mvc.perform(
            post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(task)
        ).andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andReturn()
            .response.contentAsString

        assertThat(response).isEqualTo("Invalid xpath")
    }

    @Test
    fun `post on task endpoint should validate url-xpath unique constraint`() {
        val task = Task(mockServerUrl, "//div/text()")
            .let(taskRepository::save)
            .let(::objectToString)

        val response = mvc.perform(
            post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(task)
        ).andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andReturn()
            .response.contentAsString

        assertThat(response).isEqualTo("Task with such url:xpath already exists")
    }

    @Test
    fun `post disabled task on task endpoint should save task to database and do not schedule job`() {
        val task = objectToString(Task("https://google.com", "//div", false))

        val result = mvc.perform(
            post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(task)
        ).andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andReturn()

        val createdTask = stringToObject(result.response.contentAsString, Task::class)
        assertThat(createdTask.id).isNotNull
        val taskFromDatabase = taskRepository.findById(createdTask.id!!).get()
        assertThat(createdTask).usingRecursiveComparison().isEqualTo(taskFromDatabase)

        val recordedRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS)
        assertThat(recordedRequest).isNull()
    }

    @Test
    fun `get task endpoint should return present tasks`() {
        val tasks = listOf(
            Task(mockServerUrl, "//div", true),
            Task(mockServerUrl, "/html/body", false)
        )

        taskRepository.saveAll(tasks)

        val result = mvc.perform(
            get("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andReturn()

        val receivedTasks = stringToObject(
            result.response.contentAsString,
            object : TypeReference<List<Task>>() {}
        )
        assertThat(receivedTasks).containsExactlyInAnyOrder(*tasks.toTypedArray())
    }

    @Test
    fun `patch task endpoint should modify task if present`() {
        val task = taskRepository.save(Task(mockServerUrl, "//div", false, 100))

        val result = mvc.perform(
            patch("/tasks/${task.id}")
                .content(objectToString(TaskUpdateDTO(xpath = "/html/body")))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andReturn()

        val modifiedTask = taskRepository.findById(task.id!!).get()
        val expectedTask = Task(mockServerUrl, "/html/body", false, 100L)
            .apply { id = task.id }
        assertThat(modifiedTask).usingRecursiveComparison().isEqualTo(expectedTask)

        val receivedTask = stringToObject(result.response.contentAsString, Task::class)
        assertThat(receivedTask).usingRecursiveComparison().isEqualTo(modifiedTask)
    }

    @Test
    fun `patch on task endpoint should validate task url`() {
        val task = taskRepository.save(Task(mockServerUrl, "//div"))

        val response = mvc.perform(
            patch("/tasks/${task.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectToString(TaskUpdateDTO(url = "invalid-url")))
        ).andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andReturn()
            .response.contentAsString

        assertThat(response).isEqualTo("Invalid url")
    }

    @Test
    fun `patch on task endpoint should validate task xpath`() {
        val task = taskRepository.save(Task(mockServerUrl, "//div", true))

        val response = mvc.perform(
            patch("/tasks/${task.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectToString(TaskUpdateDTO(xpath = "//div/text1()")))
        ).andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andReturn()
            .response.contentAsString

        assertThat(response).isEqualTo("Invalid xpath")
    }

    @Test
    fun `patch on task endpoint should validate url-xpath unique constraint`() {
        taskRepository.save(Task(mockServerUrl, "//div"))
        val task = Task(mockServerUrl, "//div/text()")
            .let(taskRepository::save)

        val response = mvc.perform(
            patch("/tasks/${task.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectToString(TaskUpdateDTO(xpath = "//div")))
        ).andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
            .andReturn()
            .response.contentAsString

        assertThat(response).isEqualTo("Task with such url:xpath already exists")
    }

    @Test
    fun `patch task endpoint should return not found if task is absent`() {
        mvc.perform(
            patch("/tasks/0")
                .content(objectToString(TaskUpdateDTO(null, "/html/body")))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `patch task endpoint with enabled true should schedule task`() {
        val task = taskRepository.save(Task(mockServerUrl, "//div", false, 100))

        mvc.perform(
            patch("/tasks/${task.id}")
                .content(objectToString(TaskUpdateDTO(enabled = true)))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk)

        val recordedRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS)
        assertThat(recordedRequest).isNotNull
            .extracting { it!!.method }
            .isEqualTo("GET")
    }

    @Test
    fun `patch task endpoint with enabled false should cancel task`() {
        val task = taskService.createTask(Task(mockServerUrl, "//div", true, 1000))

        mvc.perform(
            patch("/tasks/${task.id}")
                .content(objectToString(TaskUpdateDTO(enabled = false)))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk)

        verify(schedulerService).cancelTask(task.id!!)

        val firstRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS)
        assertThat(firstRequest).isNotNull
        val secondRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS)
        assertThat(secondRequest).isNull()
    }

    @Test
    fun `delete task endpoint should remove existing task`() {
        val task = taskRepository.save(Task(mockServerUrl, "//div", false, 100))
        parseResultRepository.save(ParseResult(task, "result"))

        mvc.perform(
            delete("/tasks/${task.id}")
                .content(objectToString(TaskUpdateDTO(enabled = true)))
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk)

        assertThat(taskRepository.findById(task.id!!).isEmpty)
    }

    @Test
    fun `scheduled task should produce parse results`() {
        mockWebServer.enqueue(MockResponse().setBody("<div>test</div>"))
        taskService.createTask(Task(mockServerUrl, "//div/text()", true, 1000))

        await().atMost(5, TimeUnit.SECONDS).until {
            val results = parseResultRepository.findAll()
            results.isNotEmpty()
        }
        assertThat(parseResultRepository.findAll().first().result).isEqualTo("test")
    }
}
