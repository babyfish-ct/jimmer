package org.babyfish.jimmer.sql.example.bll.error

import org.babyfish.jimmer.error.ExportedError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class CodeBasedExceptionAdvice {

    @ExceptionHandler
    fun handle(ex: BusinessException): ResponseEntity<ExportedError> =
        ResponseEntity(ex.exportedError, HttpStatus.INTERNAL_SERVER_ERROR)
}