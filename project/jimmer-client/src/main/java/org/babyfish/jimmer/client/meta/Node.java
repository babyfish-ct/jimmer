package org.babyfish.jimmer.client.meta;

public interface Node {

    void accept(Visitor visitor);
}
