package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.sql.common.AbstractTest;
import org.babyfish.jimmer.sql.model.JimmerModule;
import org.babyfish.jimmer.sql.runtime.DatabaseValidators;
import org.junit.jupiter.api.Test;

public class DatabaseValidatorTest extends AbstractTest {

    @Test
    public void testH2() {
        jdbc(con -> {
            DatabaseValidators.validate(JimmerModule.ENTITY_MANAGER, "", null, con);
        });
    }
}
