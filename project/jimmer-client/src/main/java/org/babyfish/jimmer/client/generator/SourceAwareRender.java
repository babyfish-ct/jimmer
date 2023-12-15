package org.babyfish.jimmer.client.generator;

import org.babyfish.jimmer.client.source.Source;

public interface SourceAwareRender extends Render {

    void initialize(Source source);
}
