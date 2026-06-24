package org.babyfish.jimmer.jackson.codec;

import org.babyfish.jimmer.jackson.ImmutableModuleRequiredException;
import org.babyfish.jimmer.model.BookDraft;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NoImmutableModuleTest {

    @Test
    public void test() throws Exception {
        JsonCodec<?> rawJsonCodec = JsonCodecDetector.loadJsonCodecProvider().create();
        Throwable ex = assertThrows(
                Throwable.class,
                () -> rawJsonCodec.writer().writeAsString(BookDraft.$.produce(draft -> {
                }))
        );
        assertInstanceOf(ImmutableModuleRequiredException.class, ex.getCause());
    }
}
