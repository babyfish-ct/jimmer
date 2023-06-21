package org.babyfish.jimmer.error;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ErrorDebugInfo {

    private final String message;

    private final String stackTrace;

    private final ErrorDebugInfo causeBy;

    public ErrorDebugInfo(
            @JsonProperty(value = "family", required = false) @Nullable String message,
            @JsonProperty(value = "family", required = true) @NotNull String stackTrace,
            @JsonProperty(value = "family", required = false) @Nullable ErrorDebugInfo causeBy
    ) {
        this.message = message;
        this.stackTrace = Objects.requireNonNull(stackTrace, "`stackTrace` cannot be null");
        this.causeBy = causeBy;
    }

    @Nullable
    public static ErrorDebugInfo of(@Nullable Throwable ex) {
        ErrorDebugInfo causeBy = of(ex.getCause());
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement element : ex.getStackTrace()) {
            builder.append(element.toString()).append('\n');
        }
        return new ErrorDebugInfo(
                ex.getMessage(),
                builder.toString(),
                causeBy
        );
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @NotNull
    public String getStackTrace() {
        return stackTrace;
    }

    @Nullable
    public ErrorDebugInfo getCauseBy() {
        return causeBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ErrorDebugInfo that = (ErrorDebugInfo) o;
        return Objects.equals(message, that.message) &&
                stackTrace.equals(that.stackTrace) &&
                Objects.equals(causeBy, that.causeBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, stackTrace, causeBy);
    }

    @Override
    public String toString() {
        return "ErrorDebugInfo{" +
                "message='" + message + '\'' +
                ", stackTrace='" + stackTrace + '\'' +
                ", causeBy=" + causeBy +
                '}';
    }
}
