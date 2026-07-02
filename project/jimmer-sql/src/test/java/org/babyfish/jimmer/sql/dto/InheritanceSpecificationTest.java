package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.Specification;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.query.specification.Specifications;
import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.inheritance.singletable.Client;
import org.babyfish.jimmer.sql.model.inheritance.singletable.ClientTable;
import org.babyfish.jimmer.sql.model.inheritance.singletable.Person;
import org.babyfish.jimmer.sql.model.inheritance.singletable.PersonTable;
import org.babyfish.jimmer.sql.model.inheritance.singletable.dto.ClientSpecification;
import org.babyfish.jimmer.sql.model.inheritance.singletable.dto.OrganizationSpecification;
import org.babyfish.jimmer.sql.model.inheritance.singletable.dto.PersonSpecification;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

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

    @Test
    public void testAndSpecification() {
        PersonTable table = PersonTable.$;
        PersonSpecification nameSpecification = new PersonSpecification();
        nameSpecification.setName("Bob");
        PersonSpecification firstNameSpecification = new PersonSpecification();
        firstNameSpecification.setFirstName("Bob");
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(Specifications.allOf(Arrays.asList(nameSpecification, firstNameSpecification)))
                        .select(table.id()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID " +
                                    "from CLIENT tb_1_ " +
                                    "where tb_1_.NAME = ? " +
                                    "and tb_1_.FIRST_NAME = ? " +
                                    "and tb_1_.CLIENT_TYPE = ?"
                    ).variables("Bob", "Bob", "Person");
                    ctx.rows("[101]");
                }
        );
    }

    @Test
    public void testLeafSpecificationCanBeAppliedToRootQuery() {
        ClientTable table = ClientTable.$;
        PersonSpecification specification = new PersonSpecification();
        specification.setName("Bob");
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(specification)
                        .orderBy(table.id())
                        .select(table.id()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID " +
                                    "from CLIENT tb_1_ " +
                                    "where tb_1_.CLIENT_TYPE = ? " +
                                    "and tb_1_.NAME = ? " +
                                    "order by tb_1_.ID asc"
                    ).variables("Person", "Bob");
                    ctx.rows("[101]");
                }
        );
    }

    @Test
    public void testLeafSpecificationCanUseSubtypeFieldOnRootQuery() {
        ClientTable table = ClientTable.$;
        PersonSpecification specification = new PersonSpecification();
        specification.setFirstName("Bob");
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(specification)
                        .orderBy(table.id())
                        .select(table.id()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID " +
                                    "from CLIENT tb_1_ " +
                                    "left join CLIENT tb_2_ on tb_1_.ID = tb_2_.ID and tb_2_.CLIENT_TYPE = ? " +
                                    "where tb_1_.CLIENT_TYPE = ? " +
                                    "and tb_2_.FIRST_NAME = ? " +
                                    "order by tb_1_.ID asc"
                    ).variables("Person", "Person", "Bob");
                    ctx.rows("[101]");
                }
        );
    }

    @Test
    public void testNotSpecification() {
        PersonTable table = PersonTable.$;
        PersonSpecification specification = new PersonSpecification();
        specification.setName("Bob");
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(Specifications.not(specification))
                        .select(table.id()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID " +
                                    "from CLIENT tb_1_ " +
                                    "where tb_1_.NAME <> ? " +
                                    "and tb_1_.CLIENT_TYPE = ?"
                    ).variables("Bob", "Person");
                }
        );
    }

    @Test
    public void testNotSpecificationForLeafOnRootQuery() {
        ClientTable table = ClientTable.$;
        PersonSpecification specification = new PersonSpecification();
        specification.setName("Bob");
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(Specifications.not(specification))
                        .orderBy(table.id())
                        .select(table.id()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID " +
                                    "from CLIENT tb_1_ " +
                                    "where not (tb_1_.CLIENT_TYPE = ? and tb_1_.NAME = ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("Person", "Bob");
                    ctx.rows("[100,102]");
                }
        );
    }

    @Test
    public void testOrSpecificationForInheritanceSiblings() {
        ClientTable table = ClientTable.$;
        PersonSpecification personSpecification = new PersonSpecification();
        personSpecification.setName("Acme");
        OrganizationSpecification organizationSpecification = new OrganizationSpecification();
        organizationSpecification.setName("Bob");
        Specification<Client> specification = Specifications.or(
                personSpecification,
                organizationSpecification
        );
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(specification)
                        .orderBy(table.id())
                        .select(table.id()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID " +
                                    "from CLIENT tb_1_ " +
                                    "where tb_1_.CLIENT_TYPE = ? and tb_1_.NAME = ? " +
                                    "or tb_1_.CLIENT_TYPE = ? and tb_1_.NAME = ?" +
                                    " " +
                                    "order by tb_1_.ID asc"
                    ).variables("Person", "Acme", "ORG", "Bob");
                    ctx.rows("[]");
                }
        );
    }

    @Test
    public void testOrSpecificationCanUseSubtypeFieldsForInheritanceSiblings() {
        ClientTable table = ClientTable.$;
        PersonSpecification personSpecification = new PersonSpecification();
        personSpecification.setFirstName("Bob");
        OrganizationSpecification organizationSpecification = new OrganizationSpecification();
        organizationSpecification.setTaxCode("UMB-001");
        Specification<Client> specification = Specifications.or(
                personSpecification,
                organizationSpecification
        );
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(specification)
                        .orderBy(table.id())
                        .select(table.id()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID " +
                                    "from CLIENT tb_1_ " +
                                    "left join CLIENT tb_2_ on tb_1_.ID = tb_2_.ID and tb_2_.CLIENT_TYPE = ? " +
                                    "left join CLIENT tb_3_ on tb_1_.ID = tb_3_.ID and tb_3_.CLIENT_TYPE = ? " +
                                    "where tb_1_.CLIENT_TYPE = ? and tb_2_.FIRST_NAME = ? " +
                                    "or tb_1_.CLIENT_TYPE = ? and tb_3_.TAX_CODE = ? " +
                                    "order by tb_1_.ID asc"
                    ).variables("Person", "ORG", "Person", "Bob", "ORG", "UMB-001");
                    ctx.rows("[101,102]");
                }
        );
    }

    @Test
    public void testNestedAndOrSpecificationForInheritance() {
        ClientTable table = ClientTable.$;

        ClientSpecification clientSpecification = new ClientSpecification();
        clientSpecification.setName("Bob");

        PersonSpecification personSpecification = new PersonSpecification();
        personSpecification.setFirstName("Bob");

        OrganizationSpecification organizationSpecification = new OrganizationSpecification();
        organizationSpecification.setTaxCode("UMB-001");

        Specification<Client> specification = Specifications.allOf(
                clientSpecification,
                Specifications.or(personSpecification, organizationSpecification)
        );
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(specification)
                        .orderBy(table.id())
                        .select(table.id()),
                ctx -> {
                    ctx.sql(
                            "select tb_1_.ID " +
                                    "from CLIENT tb_1_ " +
                                    "left join CLIENT tb_2_ on tb_1_.ID = tb_2_.ID and tb_2_.CLIENT_TYPE = ? " +
                                    "left join CLIENT tb_3_ on tb_1_.ID = tb_3_.ID and tb_3_.CLIENT_TYPE = ? " +
                                    "where tb_1_.NAME = ? " +
                                    "and (tb_1_.CLIENT_TYPE = ? and tb_2_.FIRST_NAME = ? " +
                                    "or tb_1_.CLIENT_TYPE = ? and tb_3_.TAX_CODE = ?) " +
                                    "order by tb_1_.ID asc"
                    ).variables("Person", "ORG", "Bob", "Person", "Bob", "ORG", "UMB-001");
                    ctx.rows("[101]");
                }
        );
    }
}
