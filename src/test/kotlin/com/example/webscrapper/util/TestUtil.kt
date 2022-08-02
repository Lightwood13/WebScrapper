package com.example.webscrapper.util

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import kotlin.reflect.KClass

private val objectMapper = ObjectMapper().registerModule(kotlinModule())

fun objectToString(value: Any): String = objectMapper.writeValueAsString(value)

fun <T : Any> stringToObject(value: String, klass: KClass<T>): T = objectMapper.readValue(value, klass.java)
fun <T : Any> stringToObject(value: String, typeReference: TypeReference<T>): T = objectMapper.readValue(value, typeReference)

class ResponsePage<T> @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    @JsonProperty("content") content: List<T>,
    @JsonProperty("number") number: Int,
    @JsonProperty("size") size: Int,
    @JsonProperty("totalElements") totalElements: Long,
    @JsonProperty("pageable") pageable: JsonNode?,
    @JsonProperty("last") last: Boolean?,
    @JsonProperty("totalPages") totalPages: Int?,
    @JsonProperty("sort") sort: JsonNode?,
    @JsonProperty("first") first: Boolean?,
    @JsonProperty("numberOfElements") numberOfElements: Int?,
    @JsonProperty("empty") empty: Boolean?
) : PageImpl<T>(
    content, PageRequest.of(number, size),
    totalElements
)