package org.babyfish.jimmer.sql.dto;

import org.babyfish.jimmer.sql.model.dto.UserView;
import org.babyfish.jimmer.sql.model.filter.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import testpkg.annotations.Serializable;

public class AnnotationTest {

    @Test
    public void testEntityAnnotations() throws NoSuchMethodException {
        Serializable a1 = UserView.class.getAnnotation(Serializable.class);
        Serializable a2 = UserView.class.getMethod("getName").getAnnotation(Serializable.class);
        Assertions.assertEquals(User.class, a1.with());
        Assertions.assertEquals(String.class, a2.with());
    }
}
