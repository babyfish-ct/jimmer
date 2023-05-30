package org.babyfish.jimmer.runtime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;

public class VisibilityTest {

    @Test
    public void test() {
        for (int i = 0; i < 257; i++) {
            test(i);
        }
    }

    private void test(int propCount) {

        Visibility visibility = Visibility.of(propCount);

        for (int propId = 0; propId < propCount; propId++) {
            Assertions.assertTrue(visibility.visible(propId));
        }
        testJdkSerialization(visibility);

        for (int propId = 0; propId < propCount; propId++) {
            if (propId % 2 != 0) {
                visibility.show(propId, false);
            }
        }
        for (int propId = 0; propId < propCount; propId++) {
            Assertions.assertEquals(propId % 2 == 0, visibility.visible(propId));
        }
        testJdkSerialization(visibility);

        for (int propId = 0; propId < propCount; propId++) {
            if (propId % 2 == 0) {
                visibility.show(propId, false);
            }
        }
        for (int propId = 0; propId < propCount; propId++) {
            Assertions.assertFalse(visibility.visible(propId));
        }
        testJdkSerialization(visibility);

        for (int propId = 0; propId < propCount; propId++) {
            if (propId % 2 != 0) {
                visibility.show(propId, true);
            }
        }
        for (int propId = 0; propId < propCount; propId++) {
            Assertions.assertEquals(propId % 2 != 0, visibility.visible(propId));
        }
        testJdkSerialization(visibility);

        for (int propId = 0; propId < propCount; propId++) {
            if (propId % 2 == 0) {
                visibility.show(propId, true);
            }
        }
        for (int propId = 0; propId < propCount; propId++) {
            Assertions.assertTrue(visibility.visible(propId));
        }
        testJdkSerialization(visibility);
    }

    private static void testJdkSerialization(Visibility visibility) {
        Visibility deserializedVisibility = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ObjectOutputStream objectOut = new ObjectOutputStream(out)) {
                objectOut.writeObject(visibility);
            }
            try (ObjectInputStream objectIn = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()))) {
                deserializedVisibility = (Visibility) objectIn.readObject();
            }
        } catch (IOException | ClassNotFoundException ex) {
            Assertions.fail(ex);
        }
        Assertions.assertEquals(visibility, deserializedVisibility);
    }
}
