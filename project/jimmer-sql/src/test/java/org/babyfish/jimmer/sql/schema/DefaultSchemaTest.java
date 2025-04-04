package org.babyfish.jimmer.sql.schema;

import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.meta.DefaultDatabaseSchemaStrategy;
import org.babyfish.jimmer.sql.model.CountryFetcher;
import org.junit.jupiter.api.Test;

public class DefaultSchemaTest extends AbstractQueryTest {

    @Test
    public void testFetchWithDefaultSchema() {
        CountryFetcher countryFetcher = CountryFetcher.$.allScalarFields();

        connectAndExpect(con ->
                getSqlClient(builder -> {
                    builder.setDatabaseSchemaStrategy(new DefaultDatabaseSchemaStrategy("public"));
                })
                        .getEntities()
                        .forConnection(con)
                        .findById(countryFetcher, "USA"), it -> {
            it.sql(
                    "select tb_1_.CODE, tb_1_.NAME " +
                    "from public.AUTHOR_COUNTRY tb_1_ " +
                    "where tb_1_.CODE = ?"
            ).variables("USA");
        });
    }
}