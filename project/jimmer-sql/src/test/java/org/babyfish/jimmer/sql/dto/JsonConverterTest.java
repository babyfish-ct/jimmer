package org.babyfish.jimmer.sql.dto;


import org.babyfish.jimmer.sql.common.AbstractQueryTest;
import org.babyfish.jimmer.sql.model.PersonalTable;
import org.babyfish.jimmer.sql.model.dto.PersonalPhoneInput;
import org.babyfish.jimmer.sql.model.dto.PersonalPhoneView;
import org.junit.jupiter.api.Test;

public class JsonConverterTest extends AbstractQueryTest {
    @Test
    public void testInput() {
        // Converter does not support the `input` method
        final PersonalPhoneInput employeePhoneInput = new PersonalPhoneInput();
        employeePhoneInput.setPhone("12345678910");
        employeePhoneInput.toEntity();
    }

    @Test
    public void testView() {
        final PersonalTable table = PersonalTable.$;
        executeAndExpect(
                getSqlClient()
                        .createQuery(table)
                        .where(table.id().eq(1L))
                        .select(table.fetch(PersonalPhoneView.class)),
                ctx -> {
                    ctx.statement(0).sql(
                            "select tb_1_.ID, tb_1_.PHONE from PERSONAL tb_1_ where tb_1_.ID = ?"
                    );
                    ctx.rows("[{\"phone\":\"123****8910\"}]");
                }
        );
    }
}
