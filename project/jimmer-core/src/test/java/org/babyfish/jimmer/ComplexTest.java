package org.babyfish.jimmer;

import org.babyfish.jimmer.model.Complex;
import org.babyfish.jimmer.model.ComplexDraft;
import org.junit.jupiter.api.Assertions;
import org.babyfish.jimmer.support.JacksonBackend;
import org.babyfish.jimmer.support.JacksonTest;

public class ComplexTest {

    @JacksonTest
    public void test(JacksonBackend backend) {
        Complex a = ComplexDraft.$.produce(c -> {
            c.setReal(5);
            c.setImage(7);
        });
        Complex b = ComplexDraft.$.produce(a, c -> {
            c.setReal(c.getReal() - 1);
            c.setImage(c.getImage() - 1);
        });
        Assertions.assertEquals(
                "{\"real\":\"5.0\",\"image\":\"7.0\"}",
                backend.write(a)
        );
        Assertions.assertEquals(
                "{\"real\":\"4.0\",\"image\":\"6.0\"}",
                backend.write(b)
        );
        Assertions.assertEquals(
                b,
                backend.read(Complex.class, "{\"real\":\"4.0\",\"image\":\"6.0\"}")
        );
    }
}
