package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.sql.common.AbstractTest;
import org.babyfish.jimmer.sql.meta.ForeignKeyStrategy;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.model.JimmerModule;
import org.babyfish.jimmer.sql.runtime.DatabaseValidationException;
import org.babyfish.jimmer.sql.runtime.DatabaseValidators;
import org.babyfish.jimmer.sql.runtime.DefaultDatabaseNamingStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DatabaseValidatorTest extends AbstractTest {

    @Test
    public void testH2() {
        jdbc(con -> {
            DatabaseValidationException ex = DatabaseValidators.validate(
                    JimmerModule.ENTITY_MANAGER,
                    "",
                    new MetadataStrategy(DefaultDatabaseNamingStrategy.UPPER_CASE, ForeignKeyStrategy.REAL),
                    "",
                    con
            );
            Assertions.assertNull(ex);
        });
    }
}
