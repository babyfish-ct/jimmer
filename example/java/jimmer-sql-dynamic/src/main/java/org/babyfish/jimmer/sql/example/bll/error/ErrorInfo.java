package org.babyfish.jimmer.sql.example.bll.error;

import java.util.Collections;
import java.util.Map;

public class ErrorInfo {

    private final ErrorCode code;

    private final Map<String, Object> extensions;

    public ErrorInfo(ErrorCode code, Map<String, Object> extensions) {
        this.code = code;
        this.extensions = extensions != null ? extensions : Collections.emptyMap();
    }

    public ErrorCode getCode() {
        return code;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    @Override
    public String toString() {
        return "ErrorInfo{" +
                "code=" + code +
                ", extensions=" + extensions +
                '}';
    }
}
