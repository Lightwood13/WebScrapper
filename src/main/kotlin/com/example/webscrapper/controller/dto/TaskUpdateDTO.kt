package com.example.webscrapper.controller.dto

data class TaskUpdateDTO(
    val url: String? = null,
    val xpath: String? = null,
    val enabled: Boolean? = null,
    val intervalMillis: Long? = null
)