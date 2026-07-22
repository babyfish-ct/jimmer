package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.impl.util.PropCache;
import org.babyfish.jimmer.impl.util.TypeCache;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.sql.di.DefaultLogicalDeletedValueGeneratorProvider;
import org.babyfish.jimmer.sql.di.DefaultUserIdGeneratorProvider;
import org.babyfish.jimmer.sql.di.LogicalDeletedValueGeneratorProvider;
import org.babyfish.jimmer.sql.di.UserIdGeneratorProvider;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.meta.impl.IdGenerators;
import org.babyfish.jimmer.sql.meta.impl.LogicalDeletedValueGenerators;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

class GeneratorManager implements GeneratorContext {

    private final Map<Class<?>, IdGenerator> idGeneratorMap;

    private final UserIdGeneratorProvider userIdGeneratorProvider;

    private final LogicalDeletedValueGeneratorProvider logicalDeletedValueGeneratorProvider;

    private final MetadataStrategy metadataStrategy;

    private final TypeCache<IdGenerator> idGeneratorCache =
            new TypeCache<>(this::createIdGenerator, true);

    private final TypeCache<LogicalDeletedValueGenerator<?>> logicalDeletedTypeCache =
            new TypeCache<>(this::createLogicalDeletedValueGenerator, true);

    private final PropCache<LogicalDeletedValueGenerator<?>> logicalDeletedPropCache =
            new PropCache<>(this::createLogicalDeletedValueGenerator, true);

    private JSqlClient sqlClient;

    GeneratorManager(
            Map<Class<?>, IdGenerator> idGeneratorMap,
            @Nullable UserIdGeneratorProvider userIdGeneratorProvider,
            @Nullable LogicalDeletedValueGeneratorProvider logicalDeletedValueGeneratorProvider,
            MetadataStrategy metadataStrategy
    ) {
        this.idGeneratorMap = idGeneratorMap;
        this.userIdGeneratorProvider =
                userIdGeneratorProvider != null ?
                        userIdGeneratorProvider :
                        new DefaultUserIdGeneratorProvider();
        this.logicalDeletedValueGeneratorProvider =
                logicalDeletedValueGeneratorProvider != null ?
                        logicalDeletedValueGeneratorProvider :
                        new DefaultLogicalDeletedValueGeneratorProvider();
        this.metadataStrategy = metadataStrategy;
    }

    void initialize(JSqlClient sqlClient) {
        this.sqlClient = sqlClient;
    }

    @Override
    public IdGenerator getIdGenerator(ImmutableType type) {
        return idGeneratorCache.get(type);
    }

    @Override
    @Nullable
    public LogicalDeletedValueGenerator<?> getLogicalDeletedValueGenerator(@Nullable LogicalDeletedInfo info) {
        if (info == null) {
            return null;
        }
        ImmutableProp prop = info.getProp();
        if (!prop.isAssociation(TargetLevel.ENTITY)) {
            return logicalDeletedTypeCache.get(prop.getDeclaringType());
        }
        ImmutableProp mappedBy = prop.getMappedBy();
        return logicalDeletedPropCache.get(mappedBy != null ? mappedBy : prop);
    }

    @Override
    public UserIdGenerator<?> getUserIdGenerator(String ref) throws Exception {
        return userIdGeneratorProvider.get(ref, sqlClient);
    }

    @SuppressWarnings("unchecked")
    @Override
    public UserIdGenerator<?> getUserIdGenerator(Class<?> userIdGeneratorType) throws Exception {
        return userIdGeneratorProvider.get((Class<UserIdGenerator<?>>) userIdGeneratorType, sqlClient);
    }

    @Override
    public LogicalDeletedValueGenerator<?> getLogicalDeletedValueGenerator(String ref) throws Exception {
        return logicalDeletedValueGeneratorProvider.get(ref, sqlClient);
    }

    @SuppressWarnings("unchecked")
    @Override
    public LogicalDeletedValueGenerator<?> getLogicalDeletedValueGenerator(
            Class<?> logicalDeletedValueGeneratorType
    ) throws Exception {
        return logicalDeletedValueGeneratorProvider.get(
                (Class<LogicalDeletedValueGenerator<?>>) logicalDeletedValueGeneratorType,
                sqlClient
        );
    }

    private IdGenerator createIdGenerator(ImmutableType type) {
        IdGenerator idGenerator = idGeneratorMap.get(type.getJavaClass());
        if (idGenerator == null) {
            idGenerator = idGeneratorMap.get(null);
        }
        return idGenerator != null ? idGenerator : IdGenerators.of(type, metadataStrategy, this);
    }

    private LogicalDeletedValueGenerator<?> createLogicalDeletedValueGenerator(ImmutableType type) {
        return LogicalDeletedValueGenerators.of(type.getLogicalDeletedInfo(), this);
    }

    private LogicalDeletedValueGenerator<?> createLogicalDeletedValueGenerator(ImmutableProp prop) {
        MiddleTable middleTable = prop.getStorage(metadataStrategy);
        return LogicalDeletedValueGenerators.of(middleTable.getLogicalDeletedInfo(), this);
    }
}
