package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.dialect.DefaultDialect;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.lang.reflect.InvocationTargetException;

@ConstructorBinding
@ConfigurationProperties("jimmer")
public class JimmerProperties {

    @NotNull
    private final String language;

    @NotNull
    private final Dialect dialect;

    @NotNull
    private final boolean showSql;

    @NotNull
    private final TriggerType triggerType;

    private final int defaultBatchSize;

    private final int defaultListBatchSize;

    @NotNull
    private final Client client;

    public JimmerProperties(
            @Nullable String language,
            @Nullable String dialect,
            boolean showSql,
            @Nullable TriggerType triggerType,
            @Nullable Integer defaultBatchSize,
            @Nullable Integer defaultListBatchSize,
            @Nullable Client client
    ) {
        if (language == null) {
            this.language = "java";
        } else {
            if (!language.equals("java") && !language.equals("kotlin")) {
                throw new IllegalArgumentException("`jimmer.language` must be \"java\" or \"kotlin\"");
            }
            this.language = language;
        }
        if (dialect == null) {
            this.dialect = new DefaultDialect();
        } else {
            Class<?> clazz;
            try {
                clazz = Class.forName(dialect, true, Dialect.class.getClassLoader());
            } catch (ClassNotFoundException ex) {
                throw new IllegalArgumentException(
                        "The class \"" +
                                dialect +
                                "\" specified by `jimmer.language` cannot be found"
                );
            }
            if (!Dialect.class.isAssignableFrom(clazz) || clazz.isInterface()) {
                throw new IllegalArgumentException(
                        "The class \"" +
                                dialect +
                                "\" specified by `jimmer.language` must be a valid dialect implementation"
                );
            }
            try {
                this.dialect = (Dialect) clazz.getConstructor().newInstance();
            } catch (InvocationTargetException ex) {
                throw new IllegalArgumentException(
                        "Create create instance for the class \"" +
                                dialect +
                                "\" specified by `jimmer.language`",
                        ex.getTargetException()
                );
            } catch (Exception ex) {
                throw new IllegalArgumentException(
                        "Create create instance for the class \"" +
                                dialect +
                                "\" specified by `jimmer.language`",
                        ex
                );
            }
        }
        this.showSql = showSql;
        this.triggerType = triggerType != null ? triggerType : TriggerType.BINLOG_ONLY;
        this.defaultBatchSize =
                defaultBatchSize != null ?
                        defaultBatchSize :
                        JSqlClient.Builder.DEFAULT_BATCH_SIZE;
        this.defaultListBatchSize =
                defaultListBatchSize != null ?
                        defaultListBatchSize :
                        JSqlClient.Builder.DEFAULT_LIST_BATCH_SIZE;
        if (client == null) {
            this.client = new Client(null);
        } else {
            this.client = client;
        }
    }

    @NotNull
    public String getLanguage() {
        return language;
    }

    @NotNull
    public Dialect getDialect() {
        return dialect;
    }

    public boolean isShowSql() {
        return showSql;
    }

    @NotNull
    public TriggerType getTriggerType() {
        return triggerType;
    }

    public int getDefaultBatchSize() {
        return defaultBatchSize;
    }

    public int getDefaultListBatchSize() {
        return defaultListBatchSize;
    }

    @NotNull
    public Client getClient() {
        return client;
    }

    @Override
    public String toString() {
        return "JimmerProperties{" +
                "language='" + language + '\'' +
                ", dialect=" + dialect +
                ", showSql=" + showSql +
                ", triggerType=" + triggerType +
                ", defaultBatchSize=" + defaultBatchSize +
                ", defaultListBatchSize=" + defaultListBatchSize +
                ", client=" + client +
                '}';
    }

    @ConstructorBinding
    public static class Client {

        @NotNull
        private final TypeScript ts;

        public Client(@Nullable TypeScript ts) {
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

        @Override
        public String toString() {
            return "Client{" +
                    "ts=" + ts +
                    '}';
        }

        @ConstructorBinding
        public static class TypeScript {

            @Nullable
            private final String path;

            public TypeScript(@Nullable String path) {
                if (path == null || path.isEmpty()) {
                    this.path = null;
                } else {
                    if (!path.startsWith("/")) {
                        throw new IllegalArgumentException("`jimmer.client.ts.path` must start with \"/\"");
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
}
