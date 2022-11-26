package org.babyfish.jimmer.apt.generator;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.GeneratorException;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.List;

public class EntityManagersGenerator {

    private final String packageName;

    private final List<TypeElement> elements;

    private final Filer filer;

    public EntityManagersGenerator(String packageName, List<TypeElement> elements, Filer filer) {
        this.packageName = packageName;
        this.elements = elements;
        this.filer = filer;
    }

    public void generate() {
        if (elements.isEmpty()) {
            return;
        }
        try {
            JavaFile
                    .builder(
                            packageName, typeSpec()
                    )
                    .indent("    ")
                    .build()
                    .writeTo(filer);
        } catch (IOException ex) {
            throw new GeneratorException("Cannot generate `EntityManagers`", ex);
        }
    }

    private TypeSpec typeSpec() {
        return TypeSpec
                .classBuilder("EntityManagers")
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
        CodeBlock.Builder builder = CodeBlock.builder()
                .add("new $T(\n", Constants.ENTITY_MANAGER_CLASS_NAME)
                .indent();
        int size = elements.size();
        for (int i = 0; i < size; i++) {
            ClassName className = ClassName.get(elements.get(i));
            if (i + 1 == size) {
                builder.add("$T.class\n", className);
            } else {
                builder.add("$T.class,\n", className);
            }
        }
        CodeBlock block = builder.unindent().add(")").build();
        return FieldSpec
                .builder(Constants.ENTITY_MANAGER_CLASS_NAME, "$")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer(block)
                .build();
    }
}
