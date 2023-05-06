package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.sql.JSqlClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ExecutorContext {

    private final StackTraceElement primaryElement;

    private final List<StackTraceElement> matchedElements;

    private final List<StackTraceElement> elements;

    private ExecutorContext(
            StackTraceElement primaryElement,
            List<StackTraceElement> matchedElements,
            List<StackTraceElement> elements
    ) {
        this.primaryElement = primaryElement;
        this.matchedElements = matchedElements;
        this.elements = elements;
    }

    @NotNull
    public StackTraceElement getPrimaryElement() {
        return primaryElement;
    }

    @NotNull
    public List<StackTraceElement> getElements() {
        return elements;
    }

    @NotNull
    public List<StackTraceElement> getMatchedElements() {
        return matchedElements;
    }

    @Nullable
    public static ExecutorContext create(JSqlClient sqlClient) {
        List<String> prefixes = ((JSqlClientImplementor)sqlClient).getExecutorContextPrefixes();
        if (prefixes == null) {
            return null;
        }
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        List<StackTraceElement> matchedElements = new ArrayList<>();
        for (StackTraceElement element : elements) {
            if (element.getLineNumber() >= 0) {
                for (String prefix : prefixes) {
                    if (element.getClassName().startsWith(prefix)) {
                        matchedElements.add(element);
                        break;
                    }
                }
            }
        }
        if (matchedElements.isEmpty()) {
            return null;
        }
        return new ExecutorContext(
                matchedElements.get(0),
                Collections.unmodifiableList(matchedElements),
                Collections.unmodifiableList(
                        Arrays.asList(elements)
                )
        );
    }
}
