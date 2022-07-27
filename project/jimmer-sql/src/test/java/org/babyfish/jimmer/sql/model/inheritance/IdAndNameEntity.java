package org.babyfish.jimmer.sql.model.inheritance;

import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Table;


@Entity
@Table(name = "BOOK_STORE")
public interface IdAndNameEntity extends IdAndName {
}
