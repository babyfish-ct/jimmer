package org.babyfish.jimmer.spring.cfg;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JimmerProperties {

    @NotNull
    private final TypeScript ts;

    public JimmerProperties(@Nullable TypeScript ts) {
        if (ts == null) {
            this.ts = new TypeScript(null);
        } else {
            this.ts = ts;
        }
    }

    @NotNull
    public TypeScript getTs() {
        return ts;
    }

    public static class TypeScript {

        @Nullable
        private final String path;

        public TypeScript(@Nullable String path) {
            if (path == null || path.isEmpty()) {
                this.path = null;
            } else {
                if (path.startsWith("/")) {
                    throw new IllegalArgumentException("`jimmer.ts.path` must start with \"/\"");
                }
                this.path = path;
            }
        }

        @Nullable
        public String getPath() {
            return path;
        }

        @Override
        public String toString() {
            return "TypeScript{" +
                    "path='" + path + '\'' +
                    '}';
        }
    }
}
