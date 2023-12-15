package org.babyfish.jimmer.client.generator;

import org.babyfish.jimmer.client.runtime.EnumType;
import org.babyfish.jimmer.client.runtime.Metadata;
import org.babyfish.jimmer.client.runtime.ObjectType;
import org.babyfish.jimmer.client.source.Source;
import org.babyfish.jimmer.client.source.SourceManager;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;

public abstract class Context {

    private final Metadata metadata;

    private final String indent;

    private final boolean isGenericSupported;

    final SourceManager sourceManager;

    protected Context(Metadata metadata, String indent) {
        this.metadata = metadata;
        this.indent = indent;
        this.isGenericSupported = determineGenericSupported();
        this.sourceManager = createSourceManager(metadata, isGenericSupported);
        for (ObjectType objectType : metadata.getStaticTypes()) {
            sourceManager.getSource(objectType);
        }
        for (ObjectType dynamicType : metadata.getDynamicTypes()) {
            sourceManager.getSource(dynamicType);
        }
        for (ObjectType fetchedType : metadata.getFetchedTypes()) {
            sourceManager.getSource(fetchedType);
        }
        for (EnumType enumType : metadata.getEnumTypes()) {
            sourceManager.getSource(enumType);
        }
    }

    public String getIndent() {
        return indent;
    }

    protected abstract boolean determineGenericSupported();

    protected abstract SourceManager createSourceManager(Metadata metadata, boolean isGenericSupported);

    protected abstract CodeWriter createCodeWriter(Context ctx, Source source);

    public Collection<Source> getRootSources() {
        return sourceManager.getRootSources();
    }

    public void render(Source source, Writer writer) {
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

    private void initialize(Source source) {

    }
}
