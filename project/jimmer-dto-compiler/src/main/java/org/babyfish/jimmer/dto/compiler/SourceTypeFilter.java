package org.babyfish.jimmer.dto.compiler;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

public final class SourceTypeFilter implements Predicate<String> {

    private final Set<String> includes;

    private final Set<String> excludes;

    public SourceTypeFilter(@Nullable String includes, @Nullable String excludes) {
        this.includes = parse(includes);
        this.excludes = parse(excludes);
    }

    @Override
    public boolean test(String qualifiedName) {
        if (!includes.isEmpty()) {
            boolean matched = false;
            for (String include : includes) {
                if (qualifiedName.startsWith(include)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        for (String exclude : excludes) {
            if (qualifiedName.startsWith(exclude)) {
                return false;
            }
        }
        return true;
    }

    private static Set<String> parse(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> values = new LinkedHashSet<>();
        for (String part : value.split("[,;]")) {
            String trimmedPart = part.trim();
            if (!trimmedPart.isEmpty()) {
                values.add(trimmedPart);
            }
        }
        return Collections.unmodifiableSet(values);
    }
}
