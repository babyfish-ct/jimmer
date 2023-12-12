package org.babyfish.jimmer.client.runtime;

import org.babyfish.jimmer.client.meta.Doc;

public interface Property {

    String getName();

    Type getType();

    Doc getDoc();
}
