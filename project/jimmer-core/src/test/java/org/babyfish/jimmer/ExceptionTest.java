package org.babyfish.jimmer;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.babyfish.jimmer.error.ClientExceptionMetadata;
import org.babyfish.jimmer.error.CodeBasedException;
import org.babyfish.jimmer.error.CodeBasedRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExceptionTest {

    @Test
    public void testException() {
        String string = new AException("hello", "world").getFields().toString();
        Assertions.assertTrue(
                "{x=hello, y=world}".equals(string) ||
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

    @Test
    public void testLeafFirstMetadataInitialization() {
        ClientExceptionMetadata leafMetadata = ClientExceptionMetadata.of(LeafException.class);
        ClientExceptionMetadata branchMetadata = leafMetadata.getSuperMetadata();
        ClientExceptionMetadata rootMetadata = branchMetadata.getSuperMetadata();

        Assertions.assertSame(leafMetadata, ClientExceptionMetadata.of(LeafException.class));
        Assertions.assertSame(branchMetadata, ClientExceptionMetadata.of(BranchException.class));
        Assertions.assertSame(rootMetadata, ClientExceptionMetadata.of(RootException.class));
        Assertions.assertSame(leafMetadata, branchMetadata.getSubMetadatas().get(0));
        Assertions.assertSame(branchMetadata, rootMetadata.getSubMetadatas().get(0));
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

    @ClientException(family = "HIERARCHY", subTypes = BranchException.class)
    private abstract static class RootException extends CodeBasedRuntimeException {
    }

    @ClientException(subTypes = LeafException.class)
    private abstract static class BranchException extends RootException {
    }

    @ClientException(code = "LEAF")
    private static class LeafException extends BranchException {
    }
}
