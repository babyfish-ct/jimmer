package org.babyfish.jimmer.impl.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

public class ObjectUtil {
    @NotNull
    @SafeVarargs
    public static <T> T firstNonNullOf(Supplier<? extends T>... suppliers) {
        return Objects.requireNonNull(firstNotNullOfOrNull(suppliers));
    }

    @Nullable
    @SafeVarargs
    public static <T> T firstNotNullOfOrNull(Supplier<? extends T>... suppliers) {
        for (Supplier<? extends T> supplier : suppliers) {
            T value = supplier.get();
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
