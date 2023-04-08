package org.babyfish.jimmer.example.cloud.model;

import org.babyfish.jimmer.sql.EnumItem;

public enum Gender {

    @EnumItem(name = "M")
    MALE,

    @EnumItem(name= "F")
    FEMALE
}
