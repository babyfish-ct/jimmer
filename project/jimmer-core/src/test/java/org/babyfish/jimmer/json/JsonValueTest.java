package org.babyfish.jimmer.json;

import org.babyfish.jimmer.support.JacksonBackend;
import org.babyfish.jimmer.support.JacksonTest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class JsonValueTest {

    private enum Level {
        LOW(10), HIGH(Long.MAX_VALUE);

        private final long value;

        Level(long value) {
            this.value = value;
        }

        @JsonValue
        public long value() {
            return value;
        }
    }

    private enum State {
        ACTIVE;

        @JsonValue
        public String code() {
            return "active";
        }
    }

    private enum Duplicate {
        FIRST, SECOND;

        @JsonValue
        public int code() {
            return 10;
        }
    }

    private enum MultipleAccessors {
        ONLY;

        @JsonValue
        public int number() {
            return 10;
        }

        @JsonValue
        public String label() {
            return "only";
        }
    }

    @JacksonTest
    public void scalarValuesRoundTrip(JacksonBackend backend) throws Exception {
        JsonCodec codec = backend.codec;
        assertEquals("\"active\"", codec.writer().writeAsString(State.ACTIVE));
        assertSame(State.ACTIVE, codec.readerFor(State.class).read("\"active\""));
        String values = "[10,9223372036854775807]";
        assertEquals(values, codec.writer().writeAsString(Arrays.asList(Level.LOW, Level.HIGH)));
        assertEquals(Arrays.asList(Level.LOW, Level.HIGH), codec.readerForListOf(Level.class).read(values));
    }

    @JacksonTest
    public void invalidRepresentations(JacksonBackend backend) {
        JsonCodec codec = backend.codec;
        for (String invalid : Arrays.asList("0", "\"10\"", "10.5", "9223372036854775808")) {
            assertThrows(Exception.class, () -> codec.readerFor(Level.class).read(invalid));
        }
        assertThrows(Exception.class, () -> codec.readerFor(State.class).read("\"ACTIVE\""));
        assertThrows(Exception.class, () -> codec.writer().writeAsString(Duplicate.FIRST));
        assertThrows(IllegalArgumentException.class, () -> EnumJson.find(MultipleAccessors.class));
    }
}
