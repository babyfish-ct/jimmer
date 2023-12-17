package org.babyfish.jimmer.client.generator;

import org.babyfish.jimmer.client.runtime.*;
import org.babyfish.jimmer.client.source.Source;
import org.babyfish.jimmer.client.source.SourceManager;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;

public abstract class Context {

    private final Metadata metadata;

    private final String indent;

    SourceManager sourceManager;

    protected Context(Metadata metadata, String indent) {
        this.metadata = metadata;
        this.indent = indent != null && !indent.isEmpty() ? indent : "    ";
    }

    private void init() {
        if (this.sourceManager != null) {
            return;
        }
        this.sourceManager = createSourceManager();
        for (Service service : metadata.getServices()) {
            sourceManager.getSource(service);
            for (Operation operation : service.getOperations()) {
                sourceManager.getSource(operation);
                Type returnType = operation.getReturnType();
                if (returnType != null) {
                    sourceManager.getSource(returnType);
                }
                for (Parameter parameter : operation.getParameters()) {
                    sourceManager.getSource(parameter.getType());
                }
                for (Type exceptionType : operation.getExceptionTypes()) {
                    sourceManager.getSource(exceptionType);
                }
            }
        }
        sourceManager.createAdditionalSources();
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public String getIndent() {
        return indent;
    }

    protected abstract SourceManager createSourceManager();

    protected abstract CodeWriter createCodeWriter(Context ctx, Source source);

    public Collection<Source> getRootSources() {
        init();
        return sourceManager.getRootSources();
    }

    public Source getRootSource(String name) {
        init();
        return sourceManager.getRootSource(name);
    }

    public Source getSource(Service service) {
        init();
        return sourceManager.getSource(service);
    }

    public Source getSource(Operation operation) {
        init();
        return sourceManager.getSource(operation);
    }

    public void render(Source source, Writer writer) {
        init();
        CodeWriter codeWriter = this.createCodeWriter(this, source);
        StringWriter headWriter = new StringWriter();
        StringWriter bodyWriter = new StringWriter();
        codeWriter.setWriter(bodyWriter);
        source.getRender().render(codeWriter);
        codeWriter.setWriter(headWriter);
        codeWriter.onFlushImportedTypes();
        try {
            String head = headWriter.toString();
            if (!head.isEmpty()) {
                writer.write(head);
                writer.write('\n');
            }
            writer.write(bodyWriter.toString());
        } catch (IOException ex) {
            throw new GeneratorException("Failed to write code for " + source);
        }
    }
}
