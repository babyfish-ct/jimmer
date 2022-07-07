package org.babyfish.jimmer.sql.event;

@FunctionalInterface
public interface AssociationListener {

    void onChange(AssociationEvent e);
}
