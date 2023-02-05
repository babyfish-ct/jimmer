package org.babyfish.jimmer.error;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

public class ExportedError {

    private final String family;

    private final String code;

    private final Map<String, Object> fields;

    @JsonCreator
    public ExportedError(
            @JsonProperty(value = "family", required = true) @NotNull String family,
            @JsonProperty(value = "code", required = true) @NotNull String code,
            @JsonProperty(value = "fields", required = true) @NotNull Map<String, Object> fields) {
        this.family = family;
        this.code = code;
        this.fields = fields;
    }

    public String getFamily() {
        return family;
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExportedError that = (ExportedError) o;
        return family.equals(that.family) && code.equals(that.code) && fields.equals(that.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(family, code, fields);
    }

    @Override
    public String toString() {
        return "ExportedError{" +
                "family='" + family + '\'' +
                ", code='" + code + '\'' +
                ", fields=" + fields +
                '}';
    }
}
