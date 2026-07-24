package org.babyfish.jimmer.apt.tuple;

import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.MetaException;
import org.babyfish.jimmer.sql.TypedTuple;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TypedTupleProcessor {

    private final Context context;

    @Nullable
    private final Set<String> delayedTupleTypeNames;

    public TypedTupleProcessor(Context context, @Nullable Set<String> delayedTupleTypeNames) {
        this.context = context;
        this.delayedTupleTypeNames = delayedTupleTypeNames;
    }

    public void process(RoundEnvironment roundEnv) {
        List<TypeElement> typeElements = new ArrayList<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(TypedTuple.class)) {
            TypeElement typeElement = (TypeElement) element;
            if (delayedTupleTypeNames == null || !delayedTupleTypeNames.contains(typeElement.getQualifiedName().toString())) {
                typeElements.add(typeElement);
            }
        }
        if (delayedTupleTypeNames != null) {
            for (String typeName : delayedTupleTypeNames) {
                TypeElement typeElement = context.getElements().getTypeElement(typeName);
                typeElements.add(typeElement);
            }
        }
        for (TypeElement typeElement : typeElements) {
            validate(typeElement);
        }
        for (TypeElement typeElement : typeElements) {
            generateTuple(typeElement);
        }
    }

    private void validate(TypeElement typeElement) {
        ElementKind kind = typeElement.getKind();
        if (kind != ElementKind.CLASS && !kind.name().equals("RECORD")) {
            throw new MetaException(
                    typeElement,
                    "The type decorated by \"@" +
                            TypedTuple.class.getName() +
                            "\" must be class or record"
            );
        }
        if (!(typeElement.getEnclosingElement() instanceof PackageElement)) {
            throw new MetaException(
                    typeElement,
                    "The type decorated by \"@" +
                            TypedTuple.class.getName() +
                            "\" must be top-level class"
            );
        }
        if (kind == ElementKind.CLASS &&
                !typeElement.getSuperclass().toString().equals("java.lang.Object")) {
            throw new MetaException(
                    typeElement,
                    "The type decorated by \"@" +
                            TypedTuple.class.getName() +
                            "\" cannot inherit other class"
            );
        }
        if (!typeElement.getTypeParameters().isEmpty()) {
            throw new MetaException(
                    typeElement,
                    "The type decorated by \"@" +
                            TypedTuple.class.getName() +
                            "\" cannot be generic type"
            );
        }
    }

    private void generateTuple(TypeElement typeElement) {
        new TypedTupleGenerator(context, typeElement).generate();
    }
}
