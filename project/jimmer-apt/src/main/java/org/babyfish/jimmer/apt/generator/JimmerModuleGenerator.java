package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.sql.Entity;

import javax.annotation.processing.Filer;
import javax.lang.model.element.*;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class JimmerModuleGenerator {

    private static final String JIMMER_ENTITIES = "META-INF/jimmer/entities";

    private final String packageName;

    private final List<TypeElement> elements;

    private final Filer filer;

    private final File jimmerEntitiesFile;

    private final Set<String> entityQualifiedNames = new TreeSet<>();

    public JimmerModuleGenerator(Collection<TypeElement> processedTypeElements, Filer filer) {
        PackageCollector packageCollector = new PackageCollector();
        for (TypeElement typeElement : processedTypeElements) {
            if (typeElement.getKind() == ElementKind.INTERFACE &&
                    typeElement.getAnnotation(Entity.class) != null) {
                packageCollector.accept(typeElement);
            }
        }
        this.elements = packageCollector.getTypeElements();
        this.filer = filer;
        FileObject fileObject;
        try {
            fileObject = filer.getResource(StandardLocation.CLASS_OUTPUT, "", JIMMER_ENTITIES);
        } catch (IOException ex) {
            throw new GeneratorException("Cannot get file object \"" + JIMMER_ENTITIES + "\"", ex);
        }
        jimmerEntitiesFile = new File(fileObject.getName());
        if (jimmerEntitiesFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(jimmerEntitiesFile))) {
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    line = line.trim();
                    if (!line.isEmpty()) {
                        entityQualifiedNames.add(line);
                        packageCollector.accept(line);
                    }
                }
            } catch (IOException ex) {
                throw new GeneratorException("Cannot read content of \"" + jimmerEntitiesFile + "\"", ex);
            }
        }
        this.packageName = packageCollector.toString();
    }

    public void generate() {

        Set<String> qualifiedNames = new TreeSet<>(entityQualifiedNames);
        for (TypeElement element : elements) {
            qualifiedNames.add(element.getQualifiedName().toString());
        }
        if (qualifiedNames.size() != entityQualifiedNames.size()) {
            jimmerEntitiesFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(jimmerEntitiesFile)) {
                for (String qualifiedName : qualifiedNames) {
                    writer.write(qualifiedName);
                    writer.write('\n');
                }
            } catch (IOException ex) {
                throw new GeneratorException("Cannot write \"" + jimmerEntitiesFile + "\"", ex);
            }
            try {
                JavaFile
                        .builder(packageName, typeSpec())
                        .indent("    ")
                        .build()
                        .writeTo(filer);
            } catch (IOException ex) {
                throw new GeneratorException("Cannot generate `EntityManagers`", ex);
            }
        }
    }

    private TypeSpec typeSpec() {
        return TypeSpec
                .classBuilder(Constants.JIMMER_MODULE)
                .addJavadoc(
                        "Under normal circumstances, users do not need to use this code. \n" +
                                "This code is for compatibility with version 0.7.47 and earlier."
                )
                .addModifiers(Modifier.PUBLIC)
                .addField(constantSpec())
                .addMethod(
                        MethodSpec
                                .constructorBuilder()
                                .addModifiers(Modifier.PRIVATE)
                                .build()
                )
                .build();
    }

    private FieldSpec constantSpec() {
        CodeBlock.Builder builder = CodeBlock
                .builder()
                .add(
                        "\n$T.fromResources(\n",
                        Constants.ENTITY_MANAGER_CLASS_NAME
                );

        builder.indent().add("JimmerModule.class.getClassLoader(),\n");
        if (packageName.isEmpty()) {
            builder.add("null\n");
        } else {
            builder.add("it -> it.getName().startsWith($S)\n", packageName + '.');
        }
        builder.unindent().add(")");

        CodeBlock block = builder.build();
        return FieldSpec
                .builder(Constants.ENTITY_MANAGER_CLASS_NAME, "ENTITY_MANAGER")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer(block)
                .build();
    }

    private static class PackageCollector {

        private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

        private List<String> pathParts;

        private String str;

        private final List<TypeElement> typeElements = new ArrayList<>();

        public void accept(TypeElement typeElement) {
            typeElements.add(typeElement);
            if (pathParts != null && pathParts.isEmpty()) {
                return;
            }
            str = null;
            for (Element parent = typeElement.getEnclosingElement(); parent != null; parent = parent.getEnclosingElement()) {
                if (parent instanceof PackageElement) {
                    String packageName = ((PackageElement) parent).getQualifiedName().toString();
                    accept(packageName);
                    break;
                }
            }
        }

        private void accept(String path) {
            List<String> parts = new ArrayList<>(Arrays.asList(DOT_PATTERN.split(path)));
            if (pathParts == null) {
                pathParts = parts;
            } else {
                int len = Math.min(pathParts.size(), parts.size());
                int index = 0;
                while (index < len) {
                    if (!pathParts.get(index).equals(parts.get(index))) {
                        break;
                    }
                    index++;
                }
                if (index < pathParts.size()) {
                    pathParts.subList(index, pathParts.size()).clear();
                }
            }
        }

        public List<TypeElement> getTypeElements() {
            return Collections.unmodifiableList(typeElements);
        }

        @Override
        public String toString() {
            String s = str;
            if (s == null) {
                List<String> ps = pathParts;
                str = s = ps == null || ps.isEmpty() ? "" : String.join(".", ps);
            }
            return s;
        }
    }
}
