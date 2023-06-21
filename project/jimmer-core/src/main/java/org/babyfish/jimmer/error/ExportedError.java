package org.babyfish.jimmer.error;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public class ExportedError {

    private final String family;

    private final String code;

    private final Map<String, Object> fields;

    private final ErrorDebugInfo debugInfo;

    @JsonCreator
    public ExportedError(
            @JsonProperty(value = "family", required = true) @NotNull String family,
            @JsonProperty(value = "code", required = true) @NotNull String code,
            @JsonProperty(value = "fields", required = true) @NotNull Map<String, Object> fields,
            @JsonProperty(value = "debugInfo", required = false) ErrorDebugInfo debugInfo
    ) {
        this.family = Objects.requireNonNull(family, "`family` cannot be null");
        this.code = Objects.requireNonNull(code, "`code` cannot be null");
        this.fields = Objects.requireNonNull(fields, "`fields` cannot be null");
        this.debugInfo = debugInfo;
    }

    @NotNull
    public String getFamily() {
        return family;
    }

    @NotNull
    public String getCode() {
        return code;
    }

    @NotNull
    public Map<String, Object> getFields() {
        return fields;
    }

    @Nullable
    public ErrorDebugInfo getDebugInfo() {
        return debugInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExportedError that = (ExportedError) o;
        return family.equals(that.family) &&
                code.equals(that.code) &&
                fields.equals(that.fields) &&
                Objects.equals(debugInfo, that.debugInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(family, code, fields, debugInfo);
    }

    @Override
    public String toString() {
        return "ExportedError{" +
                "family='" + family + '\'' +
                ", code='" + code + '\'' +
                ", fields=" + fields +
                ", debugInfo=" + debugInfo +
                '}';
    }
}
