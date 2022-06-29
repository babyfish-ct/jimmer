package org.babyfish.jimmer.sql.model.inheritance;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "BOOK_STORE")
public interface IdAndNameEntity extends IdAndName {
}
