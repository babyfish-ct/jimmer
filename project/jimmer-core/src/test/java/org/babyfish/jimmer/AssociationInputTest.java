package org.babyfish.jimmer;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.babyfish.jimmer.model.AssociationInput;
import org.babyfish.jimmer.model.AssociationInputDraft;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ListIterator;

public class AssociationInputTest {

    @Test
    public void test() throws JsonProcessingException {
        AssociationInput input = AssociationInputDraft.$.produce(i -> {
            i.setParentId(3L);
            i.childIds(true).add(10L);
            i.childIds(true).add(11L);
        });
        AssociationInput input2 = ImmutableObjects.fromString(
                AssociationInput.class,
                input.toString()
        );
        AssociationInput input3 = AssociationInputDraft.$.produce(input, i -> {
            i.setParentId(i.parentId() + 1);
            ListIterator<Long> itr = i.childIds().listIterator();
            while (itr.hasNext()) {
                itr.set(itr.next() + 1);
            }
        });

        Assertions.assertEquals(
                "{\"parentId\":3,\"childIds\":[10,11]}",
                input.toString()
        );
        Assertions.assertEquals(
                "{\"parentId\":3,\"childIds\":[10,11]}",
                input2.toString()
        );
        Assertions.assertEquals(
                "{\"parentId\":4,\"childIds\":[11,12]}",
                input3.toString()
        );
    }
}
