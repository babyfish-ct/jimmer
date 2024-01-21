package org.babyfish.jimmer.spring.cfg;

import org.babyfish.jimmer.client.generator.openapi.OpenApiProperties;
import org.babyfish.jimmer.client.generator.ts.NullRenderMode;
import org.babyfish.jimmer.sql.EnumType;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.dialect.DefaultDialect;
import org.babyfish.jimmer.sql.dialect.Dialect;
import org.babyfish.jimmer.sql.event.TriggerType;
import org.babyfish.jimmer.sql.runtime.DatabaseValidationMode;
import org.babyfish.jimmer.sql.runtime.IdOnlyTargetCheckingLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

@ConstructorBinding
@ConfigurationProperties("jimmer")
public class JimmerProperties {

    @NotNull
    private final String language;

    @NotNull
    private final Dialect dialect;

    private final boolean showSql;

    private final boolean prettySql;

    private final boolean inlineSqlVariables;

    @NotNull
    private final DatabaseValidation databaseValidation;

    @NotNull
    private final TriggerType triggerType;

    private final boolean defaultDissociationActionCheckable;

    @NotNull
    private final IdOnlyTargetCheckingLevel idOnlyTargetCheckingLevel;

    private final int transactionCacheOperatorFixedDelay;

    @NotNull
    private final EnumType.Strategy defaultEnumStrategy;

    private final int defaultBatchSize;

    private final int defaultListBatchSize;

    private final int offsetOptimizingThreshold;

    private final boolean isForeignKeyEnabledByDefault;

    private final boolean saveCommandPessimisticLock;

    private final Collection<String> executorContextPrefixes;

    @NotNull
    private final String microServiceName;

    @NotNull
    private final ErrorTranslator errorTranslator;

    @NotNull
    private final Client client;

    public JimmerProperties(
            @Nullable String language,
            @Nullable String dialect,
            boolean showSql,
            boolean prettySql,
            boolean inlineSqlVariables,
            @Deprecated @Nullable DatabaseValidationMode databaseValidationMode,
            @Nullable DatabaseValidation databaseValidation,
            @Nullable TriggerType triggerType,
            @Nullable Boolean defaultDissociationActionCheckable, // Default value is true, so use `Boolean`
            @Nullable IdOnlyTargetCheckingLevel idOnlyTargetCheckingLevel,
            @Nullable Integer transactionCacheOperatorFixedDelay,
            @Nullable EnumType.Strategy defaultEnumStrategy,
            @Nullable Integer defaultBatchSize,
            @Nullable Integer defaultListBatchSize,
            @Nullable Integer offsetOptimizingThreshold,
            @Nullable Boolean isForeignKeyEnabledByDefault, // Default value is true, so use `Boolean`
            boolean saveCommandPessimisticLock,
            @Nullable Collection<String> executorContextPrefixes,
            @Nullable String microServiceName,
            @Nullable ErrorTranslator errorTranslator,
            @Nullable Client client) {
        if (language == null) {
            this.language = "java";
        } else {
            if (!language.equals("java") && !language.equals("kotlin")) {
                throw new IllegalArgumentException("`jimmer.language` must be \"java\" or \"kotlin\"");
            }
            this.language = language;
        }
        if (dialect == null) {
            this.dialect = DefaultDialect.INSTANCE;
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
        if (prettySql && !showSql) {
            throw new IllegalArgumentException(
                    "When `pretty-sql` is true, `show-sql` must be true"
            );
        }
        if (inlineSqlVariables && !prettySql) {
            throw new IllegalArgumentException(
                    "When `inline-sql-variables` is true, `pretty-sql` must be true"
            );
        }
        this.showSql = showSql;
        this.prettySql = prettySql;
        this.inlineSqlVariables = inlineSqlVariables;
        if (databaseValidationMode != null && databaseValidation != null) {
            throw new IllegalArgumentException(
                    "Conflict configuration properties: \"jimmer.database-validation.mode\" and " +
                            "\"jimmer.database-validation-mode(deprecated)\""
            );
        }
        if (databaseValidation != null) {
            this.databaseValidation = databaseValidation;
        } else {
            this.databaseValidation =
                    new DatabaseValidation(
                            databaseValidationMode != null ?
                                    databaseValidationMode :
                                    DatabaseValidationMode.NONE,
                            null,
                            null
                    );
        }
        this.triggerType = triggerType != null ? triggerType : TriggerType.BINLOG_ONLY;
        this.defaultDissociationActionCheckable =
                defaultDissociationActionCheckable != null ?
                        defaultDissociationActionCheckable :
                        true;
        this.idOnlyTargetCheckingLevel =
                idOnlyTargetCheckingLevel != null ?
                        idOnlyTargetCheckingLevel :
                        IdOnlyTargetCheckingLevel.NONE;
        this.transactionCacheOperatorFixedDelay =
                transactionCacheOperatorFixedDelay != null ?
                        transactionCacheOperatorFixedDelay :
                        5000;
        this.defaultEnumStrategy =
                defaultEnumStrategy != null ?
                        defaultEnumStrategy :
                        EnumType.Strategy.NAME;
        this.defaultBatchSize =
                defaultBatchSize != null ?
                        defaultBatchSize :
                        JSqlClient.Builder.DEFAULT_BATCH_SIZE;
        this.defaultListBatchSize =
                defaultListBatchSize != null ?
                        defaultListBatchSize :
                        JSqlClient.Builder.DEFAULT_LIST_BATCH_SIZE;
        this.offsetOptimizingThreshold =
                offsetOptimizingThreshold != null ?
                        offsetOptimizingThreshold :
                        Integer.MAX_VALUE;
        this.isForeignKeyEnabledByDefault =
                isForeignKeyEnabledByDefault != null ?
                    isForeignKeyEnabledByDefault :
                    true;
        this.saveCommandPessimisticLock = saveCommandPessimisticLock;
        this.executorContextPrefixes = executorContextPrefixes;
        this.microServiceName =
                microServiceName != null ?
                        microServiceName :
                        "";
        if (errorTranslator == null) {
            this.errorTranslator = new ErrorTranslator(null, null, null, null);
        } else {
            this.errorTranslator = errorTranslator;
        }
        if (client == null) {
            this.client = new Client(null, null, null);
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

    public boolean isPrettySql() {
        return prettySql;
    }

    public boolean isInlineSqlVariables() {
        return inlineSqlVariables;
    }

    @NotNull
    public DatabaseValidation getDatabaseValidation() {
        return databaseValidation;
    }

    @NotNull
    public TriggerType getTriggerType() {
        return triggerType;
    }

    public boolean isDefaultDissociationActionCheckable() {
        return defaultDissociationActionCheckable;
    }

    @NotNull
    public IdOnlyTargetCheckingLevel getIdOnlyTargetCheckingLevel() {
        return idOnlyTargetCheckingLevel;
    }

    @NotNull
    public EnumType.Strategy getDefaultEnumStrategy() {
        return defaultEnumStrategy;
    }

    public int getDefaultBatchSize() {
        return defaultBatchSize;
    }

    public int getDefaultListBatchSize() {
        return defaultListBatchSize;
    }

    /**
     * For RDBMS, pagination is slow if `offset` is large, especially for MySQL.
     *
     * If `offset` >= $thisArgument
     *
     * <pre>{@code
     *  select t.* from Table t ... limit ? offset ?
     * }</pre>
     *
     * will be automatically changed to
     *
     * <pre>{@code
     *  select t.* from (
     *      select
     *          t.id as optimized_core_id_
     *      from Table t ... limit ? offset ?
     *  ) optimized_core_
     *  inner join Table as optimized_
     *      on optimized_.optimized_core_id_ = optimized_core_.optimized_core_id_
     * }</pre>
     *
     * @return An integer which is greater than 0
     */
    public int getOffsetOptimizingThreshold() {
        return offsetOptimizingThreshold;
    }

    /**
     * This configuration is only useful for {@link org.babyfish.jimmer.sql.JoinColumn}
     * of local associations (not remote associations across microservice boundaries)
     * whose `foreignKeyType` is specified as `AUTO`.Its value indicates whether the
     * foreign key is real, that is, whether there is a foreign key constraint in the database.
     *
     * <p>In general, you should ignore this configuration (defaults to true) or set it to true.</p>
     *
     * In some cases, you need to set it to false, such as
     * <ul>
     *  <li>Using database/table sharding technology, such as sharding-jdbc</li>
     *  <li>Using database that does not support foreign key, such as TiDB</li>
     * </ul>
     */
    public boolean isForeignKeyEnabledByDefault() {
        return isForeignKeyEnabledByDefault;
    }

    public boolean isSaveCommandPessimisticLock() {
        return saveCommandPessimisticLock;
    }

    /**
     * If this option is configured, when jimmer calls back
     * `org.babyfish.jimmer.sql.runtime.Executor.execute` before executing SQL,
     * it will check the stack trace information of the current thread.
     *
     * However, these stack traces have too much information, including
     * infrastructure call frames represented by jdk, jdbc driver, jimmer, and spring,
     * and the business-related information you care about will be submerged in the ocean of information.
     *
     * Through this configuration, you can specify multiple package or class prefixes, and jimmer will
     * judge whether there are some call frames in the stack trace whose class names start with some
     * of these prefixes. If the judgment is true, jimmer believes that the current callback is related
     * to your business, and the `ctx` parameter of `org.babyfish.jimmer.sql.runtime.Executor.execute`
     * will be passed as non-null.
     *
     * If the SQL logging configuration is enabled at the same time, when a SQL statement is caused by
     * the business you care about, the business call frame will be printed together with the SQL log.
     */
    @Nullable
    public Collection<String> getExecutorContextPrefixes() {
        return executorContextPrefixes;
    }

    @NotNull
    public String getMicroServiceName() {
        return microServiceName;
    }

    @NotNull
    public ErrorTranslator getErrorTranslator() {
        return errorTranslator;
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
                ", prettySql=" + prettySql +
                ", databaseValidation=" + databaseValidation +
                ", triggerType=" + triggerType +
                ", transactionCacheOperatorFixedDelay=" + transactionCacheOperatorFixedDelay +
                ", defaultEnumStrategy=" + defaultEnumStrategy +
                ", defaultBatchSize=" + defaultBatchSize +
                ", defaultListBatchSize=" + defaultListBatchSize +
                ", offsetOptimizingThreshold=" + offsetOptimizingThreshold +
                ", isForeignKeyEnabledByDefault=" + isForeignKeyEnabledByDefault +
                ", executorContextPrefixes=" + executorContextPrefixes +
                ", microServiceName='" + microServiceName + '\'' +
                ", errorTranslator=" + errorTranslator +
                ", client=" + client +
                '}';
    }

    @ConstructorBinding
    public static class DatabaseValidation {

        @NotNull
        private final DatabaseValidationMode mode;

        @Nullable
        private final String catalog;

        @Nullable
        private final String schema;

        public DatabaseValidation(
                @Nullable DatabaseValidationMode mode,
                @Nullable String catalog,
                @Nullable String schema
        ) {
            this.mode = mode != null ? mode : DatabaseValidationMode.NONE;
            this.catalog = catalog != null && !catalog.isEmpty() ? catalog : null;
            this.schema = schema != null && !schema.isEmpty() ? schema : null;
        }

        @NotNull
        public DatabaseValidationMode getMode() {
            return mode;
        }

        @Nullable
        public String getCatalog() {
            return catalog;
        }

        @Nullable
        public String getSchema() {
            return schema;
        }

        @Override
        public String toString() {
            return "Validation{" +
                    "mode=" + mode +
                    ", catalog='" + catalog + '\'' +
                    '}';
        }
    }

    @ConstructorBinding
    public static class ErrorTranslator {

        private final boolean disabled;

        private final int httpStatus;

        private final boolean debugInfoSupported;

        private final int debugInfoMaxStackTraceCount;

        public ErrorTranslator(
                Boolean disabled,
                Integer httpStatus,
                Boolean debugInfoSupported,
                Integer debugInfoMaxStackTraceCount
        ) {
            this.disabled = disabled != null ? disabled : false;
            this.httpStatus = httpStatus != null ? httpStatus : 500;
            this.debugInfoSupported = debugInfoSupported != null ? debugInfoSupported : false;
            this.debugInfoMaxStackTraceCount = debugInfoMaxStackTraceCount != null ?
                    debugInfoMaxStackTraceCount :
                    Integer.MAX_VALUE;
        }

        public boolean isDisabled() {
            return disabled;
        }

        @NotNull
        public int getHttpStatus() {
            return httpStatus;
        }

        public boolean isDebugInfoSupported() {
            return debugInfoSupported;
        }

        public int getDebugInfoMaxStackTraceCount() {
            return debugInfoMaxStackTraceCount;
        }

        @Override
        public String toString() {
            return "ErrorTranslator{" +
                    "httpStatus=" + httpStatus +
                    ", debugInfoSupported=" + debugInfoSupported +
                    '}';
        }
    }

    @ConstructorBinding
    public static class Client {

        @Nullable
        private final String uriPrefix;

        @NotNull
        private final TypeScript ts;

        @NotNull
        private final Openapi openapi;

        public Client(@Nullable String uriPrefix, @Nullable TypeScript ts, @Nullable Openapi openapi) {
            this.uriPrefix = uriPrefix;
            if (ts == null) {
                this.ts = new TypeScript(null, "Api", 4, false, null, false);
            } else {
                this.ts = ts;
            }
            if (openapi == null) {
                this.openapi = new Openapi("/openapi.yml", "/openapi.html", null);
            } else {
                this.openapi = openapi;
            }
        }

        @Nullable
        public String getUriPrefix() {
            return uriPrefix;
        }

        @NotNull
        public TypeScript getTs() {
            return ts;
        }

        @NotNull
        public Openapi getOpenapi() {
            return openapi;
        }

        @Override
        public String toString() {
            return "Client{" +
                    "ts=" + ts +
                    ", openapi=" + openapi +
                    '}';
        }

        @ConstructorBinding
        public static class TypeScript {

            @Nullable
            private final String path;

            @NotNull
            private final String apiName;

            private final int indent;

            private final boolean mutable;

            private final NullRenderMode nullRenderMode;

            private final boolean isEnumTsStyle;

            public TypeScript(
                    @Nullable String path,
                    @Nullable String apiName,
                    int indent,
                    boolean mutable,
                    @Nullable NullRenderMode nullRenderMode,
                    boolean isEnumTsStyle
            ) {
                if (path == null || path.isEmpty()) {
                    this.path = null;
                } else {
                    if (!path.startsWith("/")) {
                        throw new IllegalArgumentException("`jimmer.client.ts.path` must start with \"/\"");
                    }
                    this.path = path;
                }
                if (apiName == null || apiName.isEmpty()) {
                    this.apiName = "Api";
                } else {
                    this.apiName = apiName;
                }
                this.indent = indent != 0 ? Math.max(indent, 2) : 4;
                this.mutable = mutable;
                this.nullRenderMode = nullRenderMode != null ? nullRenderMode : NullRenderMode.UNDEFINED;
                this.isEnumTsStyle = isEnumTsStyle;
            }

            @Nullable
            public String getPath() {
                return path;
            }

            @NotNull
            public String getApiName() {
                return apiName;
            }

            public int getIndent() {
                return indent;
            }

            public boolean isMutable() {
                return mutable;
            }

            public NullRenderMode getNullRenderMode() {
                return nullRenderMode;
            }

            public boolean isEnumTsStyle() {
                return isEnumTsStyle;
            }

            @Override
            public String toString() {
                return "TypeScript{" +
                        "path='" + path + '\'' +
                        '}';
            }
        }

        @ConstructorBinding
        public static class Openapi {

            private final String path;

            private final String uiPath;

            private final OpenApiProperties properties;

            public Openapi(String path, String uiPath, OpenApiProperties properties) {
                if (path == null || path.isEmpty()) {
                    this.path = null;
                } else {
                    if (!path.startsWith("/")) {
                        throw new IllegalArgumentException("`jimmer.client.openapi.path` must start with \"/\"");
                    }
                    this.path = path;
                }
                if (uiPath == null || uiPath.isEmpty()) {
                    this.uiPath = null;
                } else {
                    if (!uiPath.startsWith("/")) {
                        throw new IllegalArgumentException("`jimmer.client.openapi.ui-path` must start with \"/\"");
                    }
                    this.uiPath = uiPath;
                }
                OpenApiProperties.Info info = properties != null ? properties.getInfo() : null;
                this.properties = OpenApiProperties.newBuilder(properties)
                        .setInfo(
                                OpenApiProperties.newInfoBuilder(properties != null ? properties.getInfo() : null)
                                        .setTitle(
                                                info != null && info.getTitle() != null ?
                                                        info.getTitle() :
                                                        "<`jimmer.client.openapi.properties.info.title` is unspecified>"
                                        )
                                        .setDescription(
                                                info != null && info.getDescription() != null ?
                                                        info.getDescription() :
                                                        "<`jimmer.client.openapi.properties.info.description` is unspecified>"
                                        )
                                        .setVersion(
                                                info != null && info.getVersion() != null ?
                                                        info.getVersion() :
                                                        "<`jimmer.client.openapi.properties.info.version` is unspecified>"
                                        )
                                        .build()
                        )
                        .build();
            }

            public String getPath() {
                return path;
            }

            public String getUiPath() {
                return uiPath;
            }

            public OpenApiProperties getProperties() {
                return properties;
            }

            @Override
            public String toString() {
                return "Openapi{" +
                        "apiPath='" + path + '\'' +
                        ", uiPath='" + uiPath + '\'' +
                        ", properties=" + properties +
                        '}';
            }
        }

        @ConstructorBinding
        public static class JavaFeign {

            @Nullable
            private final String path;

            @NotNull
            private final String apiName;

            private final int indent;

            private final String basePackage;

            public JavaFeign(@Nullable String path, @Nullable String apiName, int indent, @Nullable String basePackage) {
                if (path == null || path.isEmpty()) {
                    this.path = null;
                } else {
                    if (!path.startsWith("/")) {
                        throw new IllegalArgumentException("`jimmer.client.ts.path` must start with \"/\"");
                    }
                    this.path = path;
                }
                if (apiName == null || apiName.isEmpty()) {
                    this.apiName = "Api";
                } else {
                    this.apiName = apiName;
                }
                this.indent = indent != 0 ? Math.max(indent, 2) : 4;
                if (basePackage == null || basePackage.isEmpty()) {
                    this.basePackage = "";
                } else {
                    this.basePackage = basePackage;
                }
            }

            @Nullable
            public String getPath() {
                return path;
            }

            @NotNull
            public String getApiName() {
                return apiName;
            }

            public int getIndent() {
                return indent;
            }

            public String getBasePackage() {
                return basePackage;
            }

            @Override
            public String toString() {
                return "JavaFeign{" +
                        "path='" + path + '\'' +
                        ", clientName='" + apiName + '\'' +
                        ", indent=" + indent +
                        ", basePackage=" + basePackage +
                        '}';
            }
        }
    }
}
