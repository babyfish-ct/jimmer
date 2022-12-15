package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Document;
import org.babyfish.jimmer.client.meta.Property;
import org.babyfish.jimmer.client.meta.Type;
import org.jetbrains.annotations.Nullable;

class PropertyImpl implements Property {

    private final String name;

    private final Type type;

    private final Document document;

    PropertyImpl(String name, Type type, Document document) {
        this.name = name;
        this.type = type;
        this.document = document;
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
    public Document getDocument() {
        return document;
    }
}
