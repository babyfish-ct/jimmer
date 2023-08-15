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

    protected final JimmerProperties.ErrorTranslator errorTranslator;

    public CodeBasedExceptionAdvice(JimmerProperties properties) {
        this.errorTranslator = properties.getErrorTranslator();
        if (errorTranslator.isDebugInfoSupported()) {
            notice();
        }
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, Object>> handle(CodeBasedException ex) {
        return ResponseEntity
                .status(errorTranslator.getHttpStatus())
                .body(resultMap(ex));
    }

    protected void notice() {
        String builder = "\n" + "------------------------------------------------\n" +
                "|                                              |\n" +
                "|`jimmer.error-translator.debug-info-supported`|\n" +
                "|has been turned on, this is dangerous, please |\n" +
                "|make sure the current environment is          |\n" +
                "|NOT PRODUCTION!                               |\n" +
                "|                                              |\n" +
                "------------------------------------------------\n";
        LOGGER.info(builder);
    }

    protected Map<String, Object> resultMap(CodeBasedException ex) {
        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put(
                "family",
                StringUtil.snake(ex.getCode().getDeclaringClass().getSimpleName(), StringUtil.SnakeCase.UPPER)
        );
        resultMap.put(
                "code",
                ex.getCode().name()
        );
        resultMap.putAll(ex.getFields());
        if (errorTranslator.isDebugInfoSupported()) {
            resultMap.put("debugInfo", debugInfoMap(ex));
        }
        return resultMap;
    }

    protected Map<String, Object> debugInfoMap(Throwable ex) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("message", ex.getMessage());
        StackTraceElement[] elements = ex.getStackTrace();
        int size = Math.min(elements.length, errorTranslator.getDebugInfoMaxStackTraceCount());
        List<String> stackFrames = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            stackFrames.add(elements[i].toString());
        }
        map.put("stackFrames", stackFrames);
        if (ex.getCause() != null) {
            map.put("causeBy", debugInfoMap(ex.getCause()));
        }
        return map;
    }
}
