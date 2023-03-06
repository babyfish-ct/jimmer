package org.babyfish.jimmer.sql.common;

import org.babyfish.jimmer.sql.ast.impl.SqlExpressions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class NativeSqlTest {

    @Test
    public void testParts() {
        Assertions.assertEquals(
                "[a , <e:a>,  c , <v:a>,  d, , <e:b>, , %ee%vvv, e, , <v:b>, , f, <e:c>, , g, <v:c>,  h]",
                SqlExpressions.parts(
                        "a %e c %v d, %e, %ee%vvv, e, %v, f%e, g%v h",
                        Arrays.asList("<e:a>", "<e:b>", "<e:c>"),
                        Arrays.asList("<v:a>", "<v:b>", "<v:c>")
                ).toString()
        );
    }
}
