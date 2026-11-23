package org.babyfish.jimmer.json;

import org.babyfish.jimmer.View;
import org.babyfish.jimmer.internal.GeneratedBy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DtoJsonBindingTest {

    @Test
    public void oldGeneratedDtosRequireRegeneration() {
        IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class,
                () -> DtoJsonBinding.find(LegacyView.class)
        );
        Assertions.assertTrue(ex.getMessage().contains(LegacyView.class.getName()));
        Assertions.assertTrue(ex.getMessage().contains("Rebuild its model module"));
        Assertions.assertNull(DtoJsonBinding.find(HandWrittenView.class));
        Assertions.assertNull(DtoJsonBinding.find(Object.class));
    }

    @GeneratedBy(file = "Legacy.dto")
    private interface LegacyView extends View<Object> {}

    private interface HandWrittenView extends View<Object> {}
}
