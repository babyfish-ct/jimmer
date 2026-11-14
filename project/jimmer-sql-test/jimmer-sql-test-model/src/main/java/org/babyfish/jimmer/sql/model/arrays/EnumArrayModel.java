package org.babyfish.jimmer.sql.model.arrays;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.model.Gender;

import java.util.List;

/**
 * Testing arrays and lists of enums
 */
@Entity
public interface EnumArrayModel {

    @Id
    long id();

    Gender[] genders();

    List<Gender> genderList();

    List<Priority> priorities();
}
