package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Property;
import org.babyfish.jimmer.client.meta.Type;

class PropertyImpl implements Property {

    private final String name;

    private final Type type;

    PropertyImpl(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        return type;
    }
}
