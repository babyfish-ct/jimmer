package org.babyfish.jimmer.sql.example.graphql.entities;

import org.babyfish.jimmer.sql.EnumItem;

public enum Gender {

    @EnumItem(name = "M")
    MALE,

    @EnumItem(name = "F")
    FEMALE
}
