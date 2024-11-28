package org.babyfish.jimmer.client.generator;

import org.babyfish.jimmer.client.runtime.*;
import org.babyfish.jimmer.client.source.Source;
import org.babyfish.jimmer.client.source.SourceManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
        Set<Type> handledTypes = new HashSet<>();
        for (Service service : metadata.getServices()) {
            sourceManager.getSource(service);
            for (Operation operation : service.getOperations()) {
                sourceManager.getSource(operation);
                Type returnType = operation.getReturnType();
                if (returnType != null) {
                    initSource(returnType, handledTypes);
                }
                for (Parameter parameter : operation.getParameters()) {
                    initSource(parameter.getType(), handledTypes);
                }
                for (ObjectType exceptionType : operation.getExceptionTypes()) {
                    initSource(exceptionType, handledTypes);
                }
            }
        }
        sourceManager.createAdditionalSources();
    }

    private void initSource(Type type, Set<Type> handledTypes) {
        if (!handledTypes.add(type)) {
            return;
        }
        if (type instanceof ObjectType) {
            ObjectType objectType = (ObjectType) type;
            switch (objectType.getKind()) {
                case FETCHED:
                case DYNAMIC:
                case EMBEDDABLE:
                case STATIC:
                    sourceManager.getSource(objectType);
                    for (Type argument : objectType.getArguments()) {
                        initSource(argument, handledTypes);
                    }
                    // No break
                default:
                    for (Property property : objectType.getProperties().values()) {
                        initSource(property.getType(), handledTypes);
                    }
            }
        } else if (type instanceof NullableType) {
            initSource(((NullableType)type).getTargetType(), handledTypes);
        } else if (type instanceof ListType) {
            initSource(((ListType)type).getElementType(), handledTypes);
        } else if (type instanceof MapType) {
            initSource(((MapType)type).getKeyType(), handledTypes);
            initSource(((MapType)type).getValueType(), handledTypes);
        } else {
            sourceManager.getSource(type);
        }
    }

    private void initEnumTypes(Type type, Set<Type> handledTypes) {
        if (type instanceof ObjectType) {
            ObjectType objectType = (ObjectType) type;
            for (Property property : objectType.getProperties().values()) {
                initEnumTypes(property.getType(), handledTypes);
            }
        } else if (type instanceof EnumType) {
            initSource(type, handledTypes);
        }
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public String getIndent() {
        return indent;
    }

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

    public void renderAll(OutputStream outputStream) {
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
        try {
            renderAll(zipOutputStream);
            zipOutputStream.finish();
        } catch (IOException ex) {
            throw new GeneratorException("Cannot write source code into zip output stream", ex);
        }
    }

    private void renderAll(ZipOutputStream zipOutputStream) throws IOException {

        init();

        boolean hasOperation = false;
        for (Service service : metadata.getServices()) {
            if (!service.getOperations().isEmpty()) {
                hasOperation = true;
                break;
            }
        }
        if (!hasOperation) {
            throw new NoMetadataException();
        }

        Writer writer = new OutputStreamWriter(zipOutputStream, StandardCharsets.UTF_8);
        Map<List<String>, List<Source>> sourceMultiMap = sourceManager
                .getRootSources()
                .stream()
                .collect(
                        Collectors.groupingBy(
                                Source::getDirs,
                                LinkedHashMap::new,
                                Collectors.toList()
                        )
                );
        String suffix = '.' + getFileExtension();
        boolean isIndexRequired = isIndexRequired();
        for (Map.Entry<List<String>, List<Source>> e : sourceMultiMap.entrySet()) {
            String dir = String.join("/", e.getKey());
            if (!dir.isEmpty()) {
                dir += '/';
            }
            List<Source> sources = e.getValue();
            if (isIndexRequired) {
                zipOutputStream.putNextEntry(new ZipEntry(dir + "index" + suffix));
                renderIndex(dir, sources, writer);
                writer.flush();
            }
            for (Source source : sources) {
                zipOutputStream.putNextEntry(new ZipEntry(dir + source.getName() + suffix));
                render(source, writer);
                writer.flush();
            }
        }
    }

    public void render(Source source, Writer writer) {
        init();
        SourceWriter codeWriter = this.createCodeWriter(this, source);
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

    public void renderIndex(String dir, Writer writer) {
        if (!isIndexRequired()) {
            return;
        }
        init();
        List<String> dirs = SourceManager.dirs(dir);
        List<Source> sources = sourceManager
                .getRootSources()
                .stream()
                .filter(it -> it.getDirs().equals(dirs))
                .collect(Collectors.toList());
        renderIndex(dir, sources, writer);
    }

    private void renderIndex(String dir, List<Source> sources, Writer writer) {
        StringWriter headWriter = new StringWriter();
        StringWriter bodyWriter = new StringWriter();
        for (Source source : sources) {
            SourceWriter codeWriter = this.createCodeWriter(this, source);
            codeWriter.setWriter(bodyWriter);
            source.getRender().export(codeWriter);
            codeWriter.setWriter(headWriter);
            codeWriter.onFlushImportedTypes();
        }
        try {
            String head = headWriter.toString();
            if (!head.isEmpty()) {
                writer.write(head);
                writer.write('\n');
            }
            writer.write(bodyWriter.toString());
            renderIndexCode(dir, sources, writer);
        } catch (IOException ex) {
            throw new GeneratorException("Failed to write index for " + String.join("/", sources.get(0).getDirs()));
        }
    }

    protected abstract SourceManager createSourceManager();

    protected abstract SourceWriter createCodeWriter(Context ctx, Source source);

    protected boolean isIndexRequired() {
        return false;
    }

    protected void renderIndexCode(String dir, List<Source> sources, Writer writer) {}

    protected abstract String getFileExtension();

    protected static String indent(int indent) {
        StringBuilder indentBuilder = new StringBuilder();
        for (int i = indent; i > 0; --i) {
            indentBuilder.append(' ');
        }
        return indentBuilder.toString();
    }
}
