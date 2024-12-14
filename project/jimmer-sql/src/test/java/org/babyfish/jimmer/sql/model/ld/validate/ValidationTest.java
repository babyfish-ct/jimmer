package org.babyfish.jimmer.sql.model.ld.validate;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.LogicalDeletedInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ValidationTest {

    @Test
    public void testA() {
        ImmutableType type = ImmutableType.get(A.class);
        ImmutableProp prop = type.getProp("deleted");
        LogicalDeletedInfo info = type.getLogicalDeletedInfo();
        Assertions.assertEquals(
                false,
                prop.getDefaultValueRef().getValue()
        );
        Assertions.assertEquals(
                false,
                info.allocateInitializedValue()
        );
        Assertions.assertEquals(
                true,
                info.generateValue()
        );
    }

    @Test
    public void testB() {
        ImmutableType type = ImmutableType.get(B.class);
        ImmutableProp prop = type.getProp("active");
        LogicalDeletedInfo info = type.getLogicalDeletedInfo();
        Assertions.assertEquals(
                true,
                prop.getDefaultValueRef().getValue()
        );
        Assertions.assertEquals(
                true,
                info.allocateInitializedValue()
        );
        Assertions.assertEquals(
                false,
                info.generateValue()
        );
    }

    @Test
    public void testC() {
        ImmutableType type = ImmutableType.get(C.class);
        ImmutableProp prop = type.getProp("state");
        LogicalDeletedInfo info = type.getLogicalDeletedInfo();
        Assertions.assertEquals(
                0,
                prop.getDefaultValueRef().getValue()
        );
        Assertions.assertEquals(
                0,
                info.allocateInitializedValue()
        );
        Assertions.assertEquals(
                2,
                info.generateValue()
        );
    }

    @Test
    public void testD() {
        ImmutableType type = ImmutableType.get(D.class);
        ImmutableProp prop = type.getProp("state");
        LogicalDeletedInfo info = type.getLogicalDeletedInfo();
        Assertions.assertEquals(
                1,
                prop.getDefaultValueRef().getValue()
        );
        Assertions.assertEquals(
                1,
                info.allocateInitializedValue()
        );
        Assertions.assertEquals(
                2,
                info.generateValue()
        );
    }

    @Test
    public void testE() {
        ImmutableType type = ImmutableType.get(E.class);
        ImmutableProp prop = type.getProp("state");
        LogicalDeletedInfo info = type.getLogicalDeletedInfo();
        Assertions.assertEquals(
                State.NEW,
                prop.getDefaultValueRef().getValue()
        );
        Assertions.assertEquals(
                State.NEW,
                info.allocateInitializedValue()
        );
        Assertions.assertEquals(
                State.DELETED,
                info.generateValue()
        );
    }
}
