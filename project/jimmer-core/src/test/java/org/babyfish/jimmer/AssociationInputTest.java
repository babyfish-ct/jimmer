package org.babyfish.jimmer;

import org.babyfish.jimmer.model.AssociationInput;
import org.babyfish.jimmer.model.AssociationInputDraft;
import org.babyfish.jimmer.model.Immutables;
import org.junit.jupiter.api.Assertions;
import org.babyfish.jimmer.support.JacksonBackend;
import org.babyfish.jimmer.support.JacksonTest;

import java.util.ListIterator;

public class AssociationInputTest implements Immutables {

    @JacksonTest
    public void test(JacksonBackend backend) throws Exception {
        AssociationInput input = Immutables.createAssociationInput(i -> {
            i.setParentId(3L);
            i.childIds(true).add(10L);
            i.childIds(true).add(11L);
        });
        AssociationInput input2 = backend.read(
                AssociationInput.class,
                backend.write(input)
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
                backend.write(input)
        );
        Assertions.assertEquals(
                "{\"parentId\":3,\"childIds\":[10,11]}",
                backend.write(input2)
        );
        Assertions.assertEquals(
                "{\"parentId\":4,\"childIds\":[11,12]}",
                backend.write(input3)
        );
    }
}
