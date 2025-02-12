package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.common.AbstractTest;
import org.babyfish.jimmer.sql.dialect.H2Dialect;
import org.babyfish.jimmer.sql.meta.ForeignKeyStrategy;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.meta.ScalarTypeStrategy;
import org.babyfish.jimmer.sql.exception.DatabaseValidationException;
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
                            },
                            value -> {
                                switch (value) {
                                    case "${tables.player}":
                                        return "players";
                                    case "${columns.player.name}":
                                        return "player_name";
                                    case "${columns.player.teamId}":
                                        return "team_id";
                                }
                                return value;
                            }
                    ),
                    con
            );
            Assertions.assertNull(ex);
        });
    }
}
