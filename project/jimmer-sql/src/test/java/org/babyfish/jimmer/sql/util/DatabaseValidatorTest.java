package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.common.AbstractTest;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.meta.ForeignKeyStrategy;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.ScalarTypeStrategy;
import org.babyfish.jimmer.sql.runtime.DatabaseValidationException;
import org.babyfish.jimmer.sql.runtime.DatabaseValidators;
import org.babyfish.jimmer.sql.runtime.DefaultDatabaseNamingStrategy;
import org.babyfish.jimmer.sql.runtime.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DatabaseValidatorTest extends AbstractTest {

    @Test
    public void testH2() {
        jdbc(con -> {
            DatabaseValidationException ex = DatabaseValidators.validate(
                    EntityManager.fromResources(null, null),
                    "",
                    true,
                    new MetadataStrategy(
                            DefaultDatabaseNamingStrategy.UPPER_CASE,
                            ForeignKeyStrategy.REAL,
                            new H2Dialect(),
                            new ScalarTypeStrategy() {
                                @Override
                                public Class<?> getOverriddenSqlType(ImmutableProp prop) {
                                    return null;
                                }
                            }
                    ),
                    null,
                    null,
                    con
            );
            Assertions.assertNull(ex);
        });
    }
}
