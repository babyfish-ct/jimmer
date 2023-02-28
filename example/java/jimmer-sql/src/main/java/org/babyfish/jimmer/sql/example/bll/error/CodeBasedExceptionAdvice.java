package org.babyfish.jimmer.sql.example.bll.error;

import org.babyfish.jimmer.error.CodeBasedException;
import org.babyfish.jimmer.error.ExportedError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CodeBasedExceptionAdvice {

    @ExceptionHandler
    public ResponseEntity<ExportedError> handle(CodeBasedException ex) {
        return new ResponseEntity<>(ex.getExportedError(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
