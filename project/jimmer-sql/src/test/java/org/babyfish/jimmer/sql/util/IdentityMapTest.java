package org.babyfish.jimmer.sql.util;

import org.babyfish.jimmer.sql.ast.impl.mutation.IdentityMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IdentityMapTest {

    private IdentityMap<String, String> map;

    @BeforeEach
    public void init() {
        map = new IdentityMap<>();
    }

    @Test
    public void test() {
        String a = "A", b = "B", c = "C", d = "D";
        assertMap("{}");
        map.put(a, "Alpha");
        assertMap("{A: Alpha}");
        map.put(b, "Beta");
        assertMap("{A: Alpha, B: Beta}");
        map.put(c, "Gamma");
        assertMap("{A: Alpha, B: Beta, C: Gamma}");
        map.put(d, "Delta");
        assertMap("{A: Alpha, B: Beta, C: Gamma, D: Delta}");
        map.put(a, "alpha");
        assertMap("{A: alpha, B: Beta, C: Gamma, D: Delta}");
        map.put(c, "gamma");
        assertMap("{A: alpha, B: Beta, C: gamma, D: Delta}");
        map.replaceAll(String::toUpperCase);
        assertMap("{A: ALPHA, B: BETA, C: GAMMA, D: DELTA}");
        map.removeAll((k, v) -> k.equals(b) || k.equals(c));
        assertMap("{A: ALPHA, D: DELTA}");
        map.removeAll((k, v) -> k.equals(a));
        assertMap("{D: DELTA}");
        map.removeAll((k, v) -> k.equals(d));
        assertMap("{}");
    }

    private void assertMap(String content) {
        Assertions.assertEquals(content, map.toString());
    }
}
