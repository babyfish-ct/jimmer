package org.babyfish.jimmer.sql.runtime;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.di.LogicalDeletedValueGeneratorProvider;
import org.babyfish.jimmer.sql.di.UserIdGeneratorProvider;
import org.babyfish.jimmer.sql.meta.*;
import org.babyfish.jimmer.sql.model.Book;
import org.babyfish.jimmer.sql.model.middle.Vendor;
import org.babyfish.jimmer.sql.model.middle.VendorProps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class GeneratorCacheTest {

    @Test
    public void testIdGeneratorIsCachedPerSqlClient() {
        AtomicInteger creationCount = new AtomicInteger();
        JSqlClientImplementor sqlClient = createSqlClient(creationCount, new AtomicInteger());
        GeneratorContext generatorContext = sqlClient.getGeneratorContext();
        ImmutableType bookType = ImmutableType.get(Book.class);

        IdGenerator generator = generatorContext.getIdGenerator(bookType);

        Assertions.assertSame(generator, generatorContext.getIdGenerator(bookType));
        Assertions.assertSame(
                generator,
                sqlClient.caches(cfg -> {
                }).getGeneratorContext().getIdGenerator(bookType)
        );
        Assertions.assertEquals(1, creationCount.get());

        AtomicInteger otherCreationCount = new AtomicInteger();
        JSqlClientImplementor otherSqlClient = createSqlClient(otherCreationCount, new AtomicInteger());
        Assertions.assertNotSame(
                generator,
                otherSqlClient.getGeneratorContext().getIdGenerator(bookType)
        );
        Assertions.assertEquals(1, otherCreationCount.get());
    }

    @Test
    public void testLogicalDeletedValueGeneratorUsesTypeAndPropSlots() {
        AtomicInteger creationCount = new AtomicInteger();
        JSqlClientImplementor sqlClient = createSqlClient(new AtomicInteger(), creationCount);
        GeneratorContext generatorContext = sqlClient.getGeneratorContext();
        LogicalDeletedInfo typeInfo = ImmutableType.get(Vendor.class).getLogicalDeletedInfo();
        MiddleTable middleTable = VendorProps.VIP_SHOPS.unwrap().getStorage(sqlClient.getMetadataStrategy());

        LogicalDeletedValueGenerator<?> typeGenerator =
                generatorContext.getLogicalDeletedValueGenerator(typeInfo);
        LogicalDeletedValueGenerator<?> middleTableGenerator =
                generatorContext.getLogicalDeletedValueGenerator(middleTable.getLogicalDeletedInfo());

        Assertions.assertSame(
                typeGenerator,
                generatorContext.getLogicalDeletedValueGenerator(typeInfo)
        );
        Assertions.assertSame(
                middleTableGenerator,
                generatorContext.getLogicalDeletedValueGenerator(middleTable.getLogicalDeletedInfo())
        );
        Assertions.assertSame(
                middleTableGenerator,
                generatorContext.getLogicalDeletedValueGenerator(middleTable.getInverse().getLogicalDeletedInfo())
        );
        Assertions.assertSame(
                middleTableGenerator,
                sqlClient
                        .caches(cfg -> {
                        })
                        .getGeneratorContext()
                        .getLogicalDeletedValueGenerator(middleTable.getLogicalDeletedInfo())
        );
        Assertions.assertEquals(2, creationCount.get());
    }

    private static JSqlClientImplementor createSqlClient(
            AtomicInteger idGeneratorCreationCount,
            AtomicInteger logicalDeletedValueGeneratorCreationCount
    ) {
        return (JSqlClientImplementor) JSqlClient
                .newBuilder()
                .setUserIdGeneratorProvider(new UserIdGeneratorProvider() {
                    @Override
                    public UserIdGenerator<?> get(
                            Class<UserIdGenerator<?>> type,
                            JSqlClient sqlClient
                    ) {
                        int value = idGeneratorCreationCount.incrementAndGet();
                        return entityType -> value;
                    }
                })
                .setLogicalDeletedValueGeneratorProvider(new LogicalDeletedValueGeneratorProvider() {
                    @Override
                    public LogicalDeletedValueGenerator<?> get(
                            Class<LogicalDeletedValueGenerator<?>> type,
                            JSqlClient sqlClient
                    ) {
                        int value = logicalDeletedValueGeneratorCreationCount.incrementAndGet();
                        return () -> value;
                    }
                })
                .build();
    }
}
