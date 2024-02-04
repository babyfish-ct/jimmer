package org.babyfish.jimmer.client.java.model;

import org.babyfish.jimmer.sql.Embeddable;

@Embeddable
public interface FullName {

    String firstName();

    String lastName();
}
