package org.babyfish.jimmer.client.generator;

import org.babyfish.jimmer.client.meta.Metadata;

import java.io.OutputStream;

public interface Generator {

    void generate(Metadata metadata, OutputStream out);
}
