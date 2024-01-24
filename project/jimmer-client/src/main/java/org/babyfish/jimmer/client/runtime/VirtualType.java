package org.babyfish.jimmer.client.runtime;

import org.babyfish.jimmer.client.runtime.impl.Graph;

import java.util.Set;

public interface VirtualType extends Type {

    File FILE = new File();

    class File extends Graph implements VirtualType {

        File() {}

        @Override
        protected String toStringImpl(Set<Graph> stack) {
            return "file";
        }
    }
}
