package org.babyfish.jimmer;

import org.babyfish.jimmer.meta.impl.PropDescriptor;
import org.babyfish.jimmer.sql.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PropDescriptorTest {

    @Test
    public void testConflictByIdAndVersion() {
        IllegalArgumentException ex =
                Assertions.assertThrows(IllegalArgumentException.class, () -> {
                    PropDescriptor
                            .newBuilder(
                                    "Book",
                                    Entity.class,
                                    "id",
                                    "Long",
                                    null,
                                    false,
                                    null,
                                    null,
                                    IllegalArgumentException::new
                            )
                            .add(Id.class)
                            .add(Version.class)
                            .build();
                });
        Assertions.assertEquals(
                "Illegal property \"id\", it cannot be decorated by both " +
                        "@org.babyfish.jimmer.sql.Id and @org.babyfish.jimmer.sql.Version",
                ex.getMessage()
        );
    }

    @Test
    public void testConflictByColumnAndJoinColumn() {
        IllegalArgumentException ex =
                Assertions.assertThrows(IllegalArgumentException.class, () -> {
                    PropDescriptor
                            .newBuilder(
                                    "Book",
                                    Entity.class,
                                    "name",
                                    "String",
                                    null,
                                    false,
                                    null,
                                    null,
                                    IllegalArgumentException::new
                            )
                            .add(Column.class)
                            .add(JoinColumn.class)
                            .build();
                });
        Assertions.assertEquals(
                "Illegal property \"name\", it cannot be decorated by both " +
                        "@org.babyfish.jimmer.sql.Column and @org.babyfish.jimmer.sql.JoinColumn",
                ex.getMessage()
        );
    }

    @Test
    public void testConflictByJoinColumnAndJoinTable() {
        IllegalArgumentException ex =
                Assertions.assertThrows(IllegalArgumentException.class, () -> {
                    PropDescriptor
                            .newBuilder(
                                    "Book",
                                    Entity.class,
                                    "store",
                                    "BookStore",
                                    Entity.class,
                                    false,
                                    null,
                                    null,
                                    IllegalArgumentException::new
                            )
                            .add(JoinColumn.class)
                            .add(JoinTable.class)
                            .build();
                });
        Assertions.assertEquals(
                "Illegal property \"store\", it cannot be decorated by both " +
                        "@org.babyfish.jimmer.sql.JoinColumn and @org.babyfish.jimmer.sql.JoinTable",
                ex.getMessage()
        );
    }

    @Test
    public void testIndistinguishableByJoinColumn() {
        IllegalArgumentException ex =
                Assertions.assertThrows(IllegalArgumentException.class, () -> {
                    PropDescriptor
                            .newBuilder(
                                    "Book",
                                    Entity.class,
                                    "store",
                                    "BookStore",
                                    Entity.class,
                                    false,
                                    null,
                                    null,
                                    IllegalArgumentException::new
                            )
                            .add(JoinTable.class)
                            .build();
                });
        Assertions.assertEquals(
                "Illegal property \"store\", " +
                        "there are not enough annotations to determine that the current property " +
                        "belongs to one of the following types: [one-to-one, many-to-one, many-to-many]",
                ex.getMessage()
        );
    }

    @Test
    public void testUnexpected() {
        IllegalArgumentException ex =
                Assertions.assertThrows(IllegalArgumentException.class, () -> {
                    PropDescriptor
                            .newBuilder(
                                    "Book",
                                    Entity.class,
                                    "authors",
                                    "Author",
                                    Entity.class,
                                    true,
                                    null,
                                    null,
                                    IllegalArgumentException::new
                            )
                            .add(ManyToMany.class)
                            .add(JoinColumn.class)
                            .build();
                });
        Assertions.assertEquals(
                "Illegal property \"authors\", the many-to-many property cannot be " +
                        "decorated by @org.babyfish.jimmer.sql.JoinColumn",
                ex.getMessage()
        );
    }

    @Test
    public void testId() {
        PropDescriptor family = PropDescriptor
                .newBuilder(
                        "Book",
                        Entity.class,
                        "id",
                        "String",
                        null,
                        false,
                        null,
                        null,
                        IllegalArgumentException::new
                )
                .add(Id.class)
                .build();
        Assertions.assertEquals(PropDescriptor.Type.ID, family.getType());
    }

    @Test
    public void testVersion() {
        PropDescriptor family = PropDescriptor
                .newBuilder(
                        "Book",
                        Entity.class,
                        "version",
                        "Int",
                        null,
                        false,
                        null,
                        null,
                        IllegalArgumentException::new
                )
                .add(Version.class)
                .build();
        Assertions.assertEquals(PropDescriptor.Type.VERSION, family.getType());
    }

    @Test
    public void testBasic() {
        PropDescriptor family = PropDescriptor
                .newBuilder(
                        "Book",
                        Entity.class,
                        "name",
                        "String",
                        null,
                        false,
                        null,
                        null,
                        IllegalArgumentException::new
                )
                .add(Column.class)
                .add(Key.class)
                .build();
        Assertions.assertEquals(PropDescriptor.Type.BASIC, family.getType());
        Assertions.assertTrue(family.isPresent(Column.class));
        Assertions.assertTrue(family.isPresent(Key.class));
    }

    @Test
    public void testEmbedded() {
        PropDescriptor family = PropDescriptor
                .newBuilder(
                        "Book",
                        Entity.class,
                        "size",
                        "Int",
                        Embeddable.class,
                        false,
                        null,
                        null,
                        IllegalArgumentException::new
                )
                .add(PropOverrides.class)
                .add(Key.class)
                .build();
        Assertions.assertEquals(PropDescriptor.Type.BASIC, family.getType());
        Assertions.assertTrue(family.isPresent(PropOverrides.class));
        Assertions.assertTrue(family.isPresent(Key.class));
    }

    @Test
    public void testOneToOne() {
        PropDescriptor family = PropDescriptor
                .newBuilder(
                        "Book",
                        Entity.class,
                        "photo",
                        "String",
                        Entity.class,
                        false,
                        null,
                        null,
                        IllegalArgumentException::new
                )
                .add(OneToOne.class)
                .add(Key.class)
                .add(OnDissociate.class)
                .add(JoinColumns.class)
                .build();
        Assertions.assertEquals(PropDescriptor.Type.ONE_TO_ONE, family.getType());
        Assertions.assertTrue(family.isPresent(OneToOne.class));
        Assertions.assertTrue(family.isPresent(Key.class));
        Assertions.assertTrue(family.isPresent(OnDissociate.class));
        Assertions.assertTrue(family.isPresent(JoinColumns.class));
    }

    @Test
    public void testManyToOne() {
        PropDescriptor family = PropDescriptor
                .newBuilder(
                        "Book",
                        Entity.class,
                        "store",
                        "BookStore",
                        Entity.class,
                        false,
                        null,
                        null,
                        IllegalArgumentException::new
                )
                .add(ManyToOne.class)
                .add(Key.class)
                .add(OnDissociate.class)
                .add(JoinColumns.class)
                .build();
        Assertions.assertEquals(PropDescriptor.Type.MANY_TO_ONE, family.getType());
        Assertions.assertTrue(family.isPresent(ManyToOne.class));
        Assertions.assertTrue(family.isPresent(Key.class));
        Assertions.assertTrue(family.isPresent(OnDissociate.class));
        Assertions.assertTrue(family.isPresent(JoinColumns.class));
    }

    @Test
    public void testOneToMany() {
        PropDescriptor family = PropDescriptor
                .newBuilder(
                        "BookStore",
                        Entity.class,
                        "books",
                        "Book",
                        Entity.class,
                        true,
                        null,
                        null,
                        IllegalArgumentException::new
                )
                .add(OneToMany.class)
                .build();
        Assertions.assertEquals(PropDescriptor.Type.ONE_TO_MANY, family.getType());
        Assertions.assertTrue(family.isPresent(OneToMany.class));
    }

    @Test
    public void testManyToMany() {
        PropDescriptor family = PropDescriptor
                .newBuilder(
                        "Book",
                        Entity.class,
                        "authors",
                        "Author",
                        Entity.class,
                        true,
                        null,
                        null,
                        IllegalArgumentException::new
                )
                .add(ManyToMany.class)
                .add(JoinTable.class)
                .build();
        Assertions.assertEquals(PropDescriptor.Type.MANY_TO_MANY, family.getType());
        Assertions.assertTrue(family.isPresent(ManyToMany.class));
        Assertions.assertTrue(family.isPresent(JoinTable.class));
    }

    @Test
    public void testKeyOnly() {
        PropDescriptor family = PropDescriptor
                .newBuilder(
                        "Book",
                        Entity.class,
                        "name",
                        "String",
                        null,
                        false,
                        null,
                        null,
                        IllegalArgumentException::new
                )
                .add(Key.class)
                .build();
        Assertions.assertEquals(PropDescriptor.Type.BASIC, family.getType());
        Assertions.assertTrue(family.isPresent(Key.class));
    }
}
