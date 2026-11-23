package org.babyfish.jimmer.json;

import org.babyfish.jimmer.model.AssociationInputDraft;
import org.babyfish.jimmer.support.JacksonBackend;
import org.babyfish.jimmer.support.JacksonTest;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class NoImmutableModuleTest {

    @JacksonTest
    public void test(JacksonBackend backend) throws Exception {
        JsonCodec rawJsonCodec = backend.rawCodec();
        Throwable ex = assertThrows(
                Throwable.class,
                () -> rawJsonCodec.writer().writeAsString(AssociationInputDraft.$.produce(draft ->
                        draft.setParentId(1L).setChildIds(Collections.emptyList())
                ))
        );
        assertInstanceOf(MissingImmutableJsonBindingException.class, ex.getCause());
    }
}
