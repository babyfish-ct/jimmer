package org.babyfish.jimmer.apt.entry;

import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.GeneratorException;

import javax.annotation.processing.Filer;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.*;
import java.util.*;

public abstract class IndexFileGenerator {

    protected final Context context;

    private final Map<String, TypeElement> elementMap;

    private final File listFile;

    public IndexFileGenerator(
            Context context,
            Collection<TypeElement> typeElements,
            PackageCollector packageCollector
    ) {
        this.context = context;
        String listFilePath = getListFilePath();

        Map<String, TypeElement> elementMap = new TreeMap<>();
        for (TypeElement typeElement : typeElements) {
            if (typeElement.getKind() == ElementKind.INTERFACE) {
                if (isManaged(typeElement, true)) {
                    elementMap.put(typeElement.getQualifiedName().toString(), typeElement);
                }
                if (isManaged(typeElement, false)) {
                    packageCollector.accept(typeElement);
                }
            }
        }

        FileObject fileObject;
        try {
            fileObject = context.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", listFilePath);
        } catch (IOException ex) {
            throw new GeneratorException("Cannot get file object \"" + listFilePath + "\"", ex);
        }
        listFile = new File(fileObject.getName());
        if (listFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(listFile))) {
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    line = line.trim();
                    if (!line.isEmpty()) {
                        TypeElement typeElement = context.getElements().getTypeElement(line);
                        if (typeElement != null) {
                            if (isManaged(typeElement, true)) {
                                elementMap.put(typeElement.getQualifiedName().toString(), typeElement);
                            }
                            if (isManaged(typeElement, false)) {
                                packageCollector.accept(typeElement);
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                throw new GeneratorException("Cannot read content of \"" + listFile + "\"", ex);
            }
        }
        this.elementMap = elementMap;
    }

    public void generate() {
        listFile.getParentFile().mkdirs();
        try (Writer writer = new FileWriter(listFile)) {
            for (String qualifiedName : elementMap.keySet()) {
                writer.write(qualifiedName);
                writer.write('\n');
            }
        } catch (IOException ex) {
            throw new GeneratorException("Cannot write \"" + listFile + "\"", ex);
        }
    }

    public Map<String, TypeElement> getElementMap() {
        return Collections.unmodifiableMap(elementMap);
    }

    protected abstract String getListFilePath();

    protected abstract boolean isManaged(TypeElement typeElement, boolean strict);
}
