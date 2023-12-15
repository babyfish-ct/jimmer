package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.runtime.ObjectType;
import org.babyfish.jimmer.client.runtime.Property;
import org.babyfish.jimmer.client.runtime.Type;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class PropertyImpl extends Graph implements Property {

    private final String name;

    private final Type type;

    private final Doc doc;

    public PropertyImpl(String name, Type type, Doc doc) {
        this.name = name;
        this.type = type;
        this.doc = doc;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Nullable
    @Override
    public Doc getDoc() {
        return doc;
    }

    @Override
    protected String toStringImpl(Set<Graph> stack) {
        return name + ": " + string(type, stack);
    }
}
