package org.babyfish.jimmer;

import org.babyfish.jimmer.meta.impl.AnnotationFamily;
import org.babyfish.jimmer.sql.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import sun.jvm.hotspot.utilities.Assert;

import java.util.ArrayList;
import java.util.Arrays;

public class AnnotationFamilyTest {

    @Test
    public void testConflictByIdAndVersion() {
        AnnotationFamily.ConflictException ex =
                Assertions.assertThrows(AnnotationFamily.ConflictException.class, () -> {
                    AnnotationFamily
                            .newBuilder()
                            .add(Id.class)
                            .add(Version.class)
                            .build();
                });
        Assertions.assertSame(Id.class, ex.getAnnotationType1());
        Assertions.assertSame(Version.class, ex.getAnnotationType2());
    }

    @Test
    public void testConflictByColumnAndJoinColumn() {
        AnnotationFamily.ConflictException ex =
                Assertions.assertThrows(AnnotationFamily.ConflictException.class, () -> {
                    AnnotationFamily
                            .newBuilder()
                            .add(Column.class)
                            .add(JoinColumn.class)
                            .build();
                });
        Assertions.assertSame(Column.class, ex.getAnnotationType1());
        Assertions.assertSame(JoinColumn.class, ex.getAnnotationType2());
    }

    @Test
    public void testConflictByJoinColumnAndJoinTable() {
        AnnotationFamily.ConflictException ex =
                Assertions.assertThrows(AnnotationFamily.ConflictException.class, () -> {
                    AnnotationFamily
                            .newBuilder()
                            .add(JoinColumn.class)
                            .add(JoinTable.class)
                            .build();
                });
        Assertions.assertSame(JoinColumn.class, ex.getAnnotationType1());
        Assertions.assertSame(JoinTable.class, ex.getAnnotationType2());
    }

    @Test
    public void testIndistinguishableByJoinColumn() {
        AnnotationFamily.IndistinguishableException ex =
                Assertions.assertThrows(AnnotationFamily.IndistinguishableException.class, () -> {
                    AnnotationFamily
                            .newBuilder()
                            .add(JoinTable.class)
                            .build();
                });
        Assertions.assertEquals(
                Arrays.asList(
                        AnnotationFamily.Type.ONE_TO_ONE,
                        AnnotationFamily.Type.MANY_TO_ONE,
                        AnnotationFamily.Type.MANY_TO_MANY
                ),
                new ArrayList<>(ex.getTypes())
        );
    }

    @Test
    public void testUnexpected() {
        AnnotationFamily.UnexpectedException ex =
                Assertions.assertThrows(AnnotationFamily.UnexpectedException.class, () -> {
                    AnnotationFamily
                            .newBuilder()
                            .add(ManyToMany.class)
                            .add(JoinColumn.class)
                            .build();
                });
        Assertions.assertEquals(AnnotationFamily.Type.MANY_TO_MANY, ex.getType());
        Assertions.assertEquals(JoinColumn.class, ex.getAnnotationType());
    }

    @Test
    public void testId() throws Exception {
        AnnotationFamily family = AnnotationFamily
                .newBuilder()
                .add(Id.class)
                .build();
        Assertions.assertEquals(AnnotationFamily.Type.ID, family.getType());
    }

    @Test
    public void testVersion() throws Exception {
        AnnotationFamily family = AnnotationFamily
                .newBuilder()
                .add(Version.class)
                .build();
        Assertions.assertEquals(AnnotationFamily.Type.VERSION, family.getType());
    }
}
