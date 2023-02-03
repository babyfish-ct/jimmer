package org.babyfish.jimmer.sql.example.bll.error;

import java.util.Arrays;
import java.util.List;

public enum ErrorCode {

    NO_GLOBAL_TENANT;

    private List<Extension> extensions;

    private ErrorCode(Extension... extensions) {
        this.extensions = Arrays.asList(extensions);
    }

    public static class Extension {

        private final String name;

        private final Class<?> type;

        public Extension(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public Class<?> getType() {
            return type;
        }
    }
}
