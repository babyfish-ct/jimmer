package org.babyfish.jimmer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.jackson.ImmutableModuleRequiredException;
import org.babyfish.jimmer.model.BookDraft;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NoImmutableModuleTest {

    @Test
    public void test() throws JsonProcessingException {
        Throwable ex = Assertions.assertThrows(Throwable.class, () -> {
            new ObjectMapper().writeValueAsString(
                    BookDraft.$.produce(draft -> {
                    })
            );
        });
        Assertions.assertInstanceOf(
                ImmutableModuleRequiredException.class,
                ex.getCause()
        );
    }
}
