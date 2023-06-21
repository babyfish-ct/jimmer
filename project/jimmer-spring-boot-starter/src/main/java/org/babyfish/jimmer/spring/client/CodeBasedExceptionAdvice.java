package org.babyfish.jimmer.spring.client;

import org.babyfish.jimmer.error.CodeBasedException;
import org.babyfish.jimmer.error.ExportedError;
import org.babyfish.jimmer.spring.cfg.JimmerProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CodeBasedExceptionAdvice {

    private final JimmerProperties.ErrorWrapper errorWrapper;

    public CodeBasedExceptionAdvice(JimmerProperties properties) {
        this.errorWrapper = properties.getErrorWrapper();
    }

    @ExceptionHandler
    public ResponseEntity<ExportedError> handle(CodeBasedException ex) {
        return new ResponseEntity<>(
                ex.toExportedError(errorWrapper.isDebugInfoSupported()),
                errorWrapper.getHttpStatus()
        );
    }
}
