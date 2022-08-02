package com.example.webscrapper.exception

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class CustomExceptionHandler {

    @ExceptionHandler(value = [NotFoundException::class])
    fun handleNotFoundException(exception: NotFoundException): ResponseEntity<Void> =
        ResponseEntity.notFound().build()

    @ExceptionHandler(value = [IllegalArgumentException::class])
    fun handleIllegalArgumentException(exception: IllegalArgumentException): ResponseEntity<String> =
        ResponseEntity.badRequest().body(exception.message)
}