package org.babyfish.jimmer.apt.entry;

import com.squareup.javapoet.*;
import org.babyfish.jimmer.apt.GeneratorException;
import org.babyfish.jimmer.apt.immutable.generator.Constants;
import org.babyfish.jimmer.apt.util.ClassNames;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.Collection;

import static org.babyfish.jimmer.apt.util.GeneratedAnnotation.generatedAnnotation;

public class ObjectsGenerator extends AbstractSummaryGenerator {

    private final String packageName;

    private final String simpleName;

    private final Collection<TypeElement> typeElements;

    private final Filer filer;

    public ObjectsGenerator(String packageName, String simpleName, Collection<TypeElement> typeElements, Filer filer) {
        this.packageName = packageName;
        this.typeElements = typeElements;
        this.simpleName = simpleName;
        this.filer = filer;
    }

    public void generate() {
        TypeSpec typeSpec = typeSpec();
        try {
            JavaFile
                    .builder(
                            packageName,
                            typeSpec
                    )
                    .indent("    ")
                    .build()
                    .writeTo(filer);
        } catch (IOException ex) {
            throw new GeneratorException(
                    String.format(
                            "Cannot generate draft interface for '%s'",
                            packageName + '.' + simpleName
                    ),
                    ex
            );
        }
    }

    private TypeSpec typeSpec() {
        TypeSpec.Builder builder = TypeSpec
                .interfaceBuilder(ClassName.get(packageName, simpleName))
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(generatedAnnotation());
        for (TypeElement typeElement : typeElements) {
            String methodName = distinctName("create" + typeElement.getSimpleName().toString());
            builder.addMethod(creator(typeElement, methodName, false));
            builder.addMethod(creator(typeElement, methodName,true));
        }
        return builder.build();
    }

    private MethodSpec creator(TypeElement typeElement, String methodName, boolean withBase) {
        ClassName immutableClassName = ClassName.get(typeElement);
        ClassName draftClassName = ClassNames.of(typeElement, name -> name + "Draft");
        MethodSpec.Builder builder = MethodSpec
                .methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        if (withBase) {
            builder.addParameter(immutableClassName, "base");
        }
        builder
                .addParameter(
                        ParameterizedTypeName.get(
                                Constants.DRAFT_CONSUMER_CLASS_NAME,
                                draftClassName
                        ),
                        "block"
                )
                .returns(ClassName.get(typeElement));
        if (withBase) {
            builder.addStatement("return $T.$$.produce(base, block)", draftClassName);
        } else {
            builder.addStatement("return $T.$$.produce(block)", draftClassName);
        }
        return builder.build();
    }
}
