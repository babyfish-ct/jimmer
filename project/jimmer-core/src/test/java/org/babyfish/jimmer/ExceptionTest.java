package org.babyfish.jimmer;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.babyfish.jimmer.error.CodeBasedException;
import org.babyfish.jimmer.error.CodeBasedRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExceptionTest {

    @Test
    public void testException() {
        String string = new AException("hello", "world").getFields().toString();
        Assertions.assertTrue(
                "{x=hello, y=world}".equals(string)||
                        "{y=world, x=hello}".equals(string)

        );
    }

    @Test
    public void testRuntimeException() {
        String string = new BException("hello", "world").getFields().toString();
        Assertions.assertTrue(
                "{x=hello, y=world}".equals(string) ||
                        "{y=world, x=hello}".equals(string)

        );
    }

    @ClientException(code = "A")
    @JsonPropertyOrder({"x", "y"})
    private static class AException extends CodeBasedException {

        private final String x;

        private final String y;

        private AException(String x, String y) {
            this.x = x;
            this.y = y;
        }

        public String getX() {
            return x;
        }

        public String getY() {
            return y;
        }
    }

    @ClientException(code = "B")
    private static class BException extends CodeBasedRuntimeException {

        private final String x;

        private final String y;

        private BException(String x, String y) {
            this.x = x;
            this.y = y;
        }

        public String getX() {
            return x;
        }

        public String getY() {
            return y;
        }
    }
}
