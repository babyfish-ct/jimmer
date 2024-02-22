package org.babyfish.jimmer.client.generator.java;

import org.babyfish.jimmer.client.generator.Render;
import org.babyfish.jimmer.client.generator.SourceWriter;
import org.babyfish.jimmer.client.runtime.Service;

public class ServiceImplRender implements Render {

    private final Service service;

    public ServiceImplRender(Service service) {
        this.service = service;
    }

    @Override
    public void render(SourceWriter writer) {

    }
}
