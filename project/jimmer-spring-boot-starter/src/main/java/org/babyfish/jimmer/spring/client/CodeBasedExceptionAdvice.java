package org.babyfish.jimmer.spring.client;

import org.babyfish.jimmer.error.CodeBasedException;
import org.babyfish.jimmer.impl.util.StringUtil;
import org.babyfish.jimmer.spring.cfg.JimmerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.*;

@ControllerAdvice
public class CodeBasedExceptionAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodeBasedExceptionAdvice.class);

    private final JimmerProperties.ErrorTranslator errorTranslator;

    public CodeBasedExceptionAdvice(JimmerProperties properties) {
        this.errorTranslator = properties.getErrorTranslator();
        if (errorTranslator.isDebugInfoSupported()) {
            StringBuilder builder = new StringBuilder("\n");
            builder.append("------------------------------------------------\n");
            builder.append("|                                              |\n");
            builder.append("|`jimmer.error-translator.debug-info-supported`|\n");
            builder.append("|has been turned on, this is dangerous, please |\n");
            builder.append("|make sure the current environment is          |\n");
            builder.append("|NOT PRODUCTION!                               |\n");
            builder.append("|                                              |\n");
            builder.append("------------------------------------------------\n");
            LOGGER.info(builder.toString());
        }
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, Object>> handle(CodeBasedException ex) {
        Map<String, Object> outputMap = new LinkedHashMap<>();
        outputMap.put(
                "family",
                StringUtil.snake(ex.getCode().getDeclaringClass().getSimpleName(), StringUtil.SnakeCase.UPPER)
        );
        outputMap.put(
                "code",
                ex.getCode().name()
        );
        outputMap.putAll(ex.getFields());
        if (errorTranslator.isDebugInfoSupported()) {
            outputMap.put("debugInfo", debugInfo(ex));
        }
        return new ResponseEntity<>(
                outputMap,
                errorTranslator.getHttpStatus()
        );
    }

    private static Map<String, Object> debugInfo(Throwable ex) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("message", ex.getMessage());
        List<String> stackFrames = new ArrayList<>();
        for (StackTraceElement element : ex.getStackTrace()) {
            stackFrames.add(element.toString());
        }
        map.put("stackFrames", stackFrames);
        if (ex.getCause() != null) {
            map.put("causeBy", debugInfo(ex.getCause()));
        }
        return map;
    }
}
