package org.babyfish.jimmer.json.codec;

import org.babyfish.jimmer.json.ImmutableSerializationException;
import org.babyfish.jimmer.model.BookDraft;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NoImmutableSerializationSupportTest {

    @Test
    public void test() throws Exception {
        JsonCodec rawJsonCodec = JsonCodecDetector.loadJsonCodecProvider().codec();
        Throwable ex = assertThrows(
                Throwable.class,
                () -> rawJsonCodec.writer().writeAsString(BookDraft.$.produce(draft -> {
                }))
        );
        assertInstanceOf(ImmutableSerializationException.class, ex.getCause());
    }
}
