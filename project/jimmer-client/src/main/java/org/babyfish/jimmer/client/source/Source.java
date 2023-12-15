package org.babyfish.jimmer.client.source;

import org.babyfish.jimmer.client.generator.Render;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public interface Source {

    List<String> getDirs();

    String getName();

    Render getRender();

    @Nullable
    Source getParent();

    Source getRoot();

    Source subSource(String name, Supplier<Render> renderSupplier);

    Collection<Source> getSubSources();
}
