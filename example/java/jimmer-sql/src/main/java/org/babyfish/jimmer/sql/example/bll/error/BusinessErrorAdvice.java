package org.babyfish.jimmer.sql.example.bll.error;

import org.babyfish.jimmer.error.ExportedError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class BusinessErrorAdvice {

    @ExceptionHandler
    public ResponseEntity<ExportedError> handle(BusinessException ex) {
        return new ResponseEntity<>(ex.getExportedError(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
