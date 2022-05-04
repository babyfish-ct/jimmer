package org.babyfish.jimmer;

import org.babyfish.jimmer.model.Complex;
import org.babyfish.jimmer.model.ComplexDraft;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ComplexTest {

    @Test
    public void test() {
        Complex a = ComplexDraft.$.produce(c -> {
            c.setReal(5);
            c.setImage(7);
        });
        Complex b = ComplexDraft.$.produce(a, c -> {
           c.setReal(c.getReal() - 1);
           c.setImage(c.getImage() - 1);
        });
        Assertions.assertEquals(
                "{\"real\":5.0,\"image\":7.0}",
                a.toString()
        );
        Assertions.assertEquals(
                "{\"real\":4.0,\"image\":6.0}",
                b.toString()
        );
    }
}
