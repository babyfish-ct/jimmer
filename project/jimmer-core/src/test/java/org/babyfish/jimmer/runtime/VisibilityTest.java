package org.babyfish.jimmer.runtime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VisibilityTest {

    @Test
    public void test() {
        for (int i = 1; i <= 257; i++) {
            test(i);
        }
    }

    private void test(int propCount) {

        Visibility visibility = Visibility.of(propCount);

        for (int propId = 1; propId <= propCount; propId++) {
            Assertions.assertTrue(visibility.visible(propId));
        }

        for (int propId = 1; propId <= propCount; propId++) {
            if (propId % 2 != 0) {
                visibility.show(propId, false);
            }
        }
        for (int propId = 1; propId <= propCount; propId++) {
            Assertions.assertEquals(propId % 2 == 0, visibility.visible(propId));
        }

        for (int propId = 1; propId <= propCount; propId++) {
            if (propId % 2 == 0) {
                visibility.show(propId, false);
            }
        }
        for (int propId = 1; propId <= propCount; propId++) {
            Assertions.assertFalse(visibility.visible(propId));
        }

        for (int propId = 1; propId <= propCount; propId++) {
            if (propId % 2 != 0) {
                visibility.show(propId, true);
            }
        }
        for (int propId = 1; propId <= propCount; propId++) {
            Assertions.assertEquals(propId % 2 != 0, visibility.visible(propId));
        }

        for (int propId = 1; propId <= propCount; propId++) {
            if (propId % 2 == 0) {
                visibility.show(propId, true);
            }
        }
        for (int propId = 1; propId <= propCount; propId++) {
            Assertions.assertTrue(visibility.visible(propId));
        }
    }
}
