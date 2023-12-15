package org.babyfish.jimmer.client.source;

import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.generator.SourceAwareRender;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

abstract class AbstractSource implements Source {

    private final Render render;

    private Map<String, Source> subSourceMap;

    AbstractSource(Render render) {
        this.render = Objects.requireNonNull(render, "render cannot be null");
    }

    @Override
    public Source subSource(String name, Supplier<Render> renderCreator) {
        Map<String, Source> subSourceMap = this.subSourceMap;
        if (subSourceMap == null) {
            this.subSourceMap = subSourceMap = new HashMap<>();
        }
        Source subSource = subSourceMap.get(name);
        if (subSource == null) {
            subSource = new SubSource(name, renderCreator.get());
            subSourceMap.put(name, subSource);
            bindRenderBackReference(subSource);
        }
        return subSource;
    }

    @Override
    public Collection<Source> getSubSources() {
        if (subSourceMap == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(subSourceMap.values());
    }

    @Override
    public Render getRender() {
        return render;
    }

    @Override
    public int hashCode() {
        return getDirs().hashCode() ^ getName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AbstractSource)) {
            return false;
        }
        AbstractSource other = (AbstractSource) obj;
        return this.getDirs().equals(other.getDirs()) &&
                this.getName().equals(other.getName());
    }

    @Override
    public String toString() {
        return toString(getDirs(), getName());
    }

    static String toString(List<String> dirs, String name) {
        return String.join("/", dirs) + '/' + name;
    }

    static void bindRenderBackReference(Source source) {
        Render render = source.getRender();
        if (render instanceof SourceAwareRender) {
            ((SourceAwareRender)render).initialize(source);
        }
    }

    private class SubSource extends AbstractSource {

        private final String name;

        private final Source root;

        private SubSource(String name, Render render) {
            super(render);
            this.name = name;
            this.root = AbstractSource.this.getRoot();
        }

        @Override
        public List<String> getDirs() {
            return AbstractSource.this.getDirs();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Source getParent() {
            return AbstractSource.this;
        }

        @Override
        public Source getRoot() {
            return root;
        }
    }
}
