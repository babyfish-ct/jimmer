package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance.singletable.Person;
import org.babyfish.jimmer.sql.model.inheritance.singletable.PersonTable;
import org.babyfish.jimmer.sql.model.inheritance.singletable.dto.PersonSpecification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InheritanceSpecificationTest extends AbstractQueryTest {

    @Test
    public void testLeafSpecificationIsOrdinarySpecification() {
        PersonTable table = PersonTable.$;
        PersonSpecification specification = new PersonSpecification();
        specification.setName("Bob");
        specification.setFirstName("Bob");
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(specification)
                        .select(table),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID, tb_1_.CLIENT_TYPE, tb_1_.NAME, tb_1_.FIRST_NAME, tb_1_.LAST_NAME " +
                                    "from CLIENT tb_1_ " +
                                    "where tb_1_.NAME = ? " +
                                    "and tb_1_.FIRST_NAME = ? " +
                                    "and tb_1_.CLIENT_TYPE = ?"
                    ).variables("Bob", "Bob", "Person");
                    ctx.row(0, row -> {
                        assertEquals(Person.class, ((ImmutableSpi) row).__type().getJavaClass());
                        assertEquals(101L, row.id());
                        assertEquals("Bob", row.name());
                        assertEquals("Bob", row.firstName());
                        assertEquals("Brown", row.lastName());
                    });
                }
        );
    }
}
