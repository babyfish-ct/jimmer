package org.babyfish.jimmer.client.source;

import org.babyfish.jimmer.client.generator.Render;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class SourceFile extends AbstractSource {

    private final List<String> dirs;

    private final String name;

    public SourceFile(List<String> dirs, String name, Render render) {
        super(render);
        this.dirs = dirs;
        this.name = name;
    }

    @Override
    public List<String> getDirs() {
        return dirs;
    }

    @Override
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public Source getParent() {
        return null;
    }

    @Override
    public Source getRoot() {
        return this;
    }
}
