package org.babyfish.jimmer.apt.transactional;

import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.MetaException;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class TxProcessor {

    private final Context ctx;

    public TxProcessor(Context ctx) {
        this.ctx = ctx;
    }

    public void process(RoundEnvironment roundEnv) {
        if (ctx.isBuddyIgnoreResourceGeneration()) {
            return;
        }
        TypeElement txElement = ctx.getElements().getTypeElement(TxUtil.TX);
        if (txElement == null) {
            return;
        }
        Map<String, TypeElement> map = new LinkedHashMap<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(txElement)) {
            if (TxUtil.tx(ctx, element) != null) {
                if (element instanceof TypeElement) {
                    TypeElement typeElement = (TypeElement) element;
                    map.computeIfAbsent(typeElement.getQualifiedName().toString(), it -> validateType(typeElement));
                } else if (element instanceof ExecutableElement) {
                    TypeElement typeElement = (TypeElement) element.getEnclosingElement();
                    map.computeIfAbsent(typeElement.getQualifiedName().toString(), it -> validateType(typeElement));
                }
            }
        }
        for (TypeElement typeElement : map.values()) {
            new TxGenerator(ctx, typeElement).generate();
        }
    }

    private TypeElement validateType(Element element) {
        TypeElement typeElement = (TypeElement) element;
        if (typeElement.getKind() != ElementKind.CLASS) {
            throw new MetaException(
                    typeElement,
                    "The type uses `@Tx` must be class"
            );
        }
        if (!(typeElement.getEnclosingElement() instanceof PackageElement)) {
            throw new MetaException(
                    typeElement,
                    "The class uses `@Tx` must be top-level class"
            );
        }
        if (typeElement.getModifiers().contains(Modifier.FINAL)) {
            throw new MetaException(
                    typeElement,
                    "The class uses by `@Tx` cannot be final"
            );
        }
        if (!typeElement.getTypeParameters().isEmpty()) {
            throw new MetaException(
                    typeElement,
                    "The current version does not yet support the use of generics for types annotated with @Tx"
            );
        }
        if (!ctx.isObject(typeElement.getSuperclass())) {
            throw new MetaException(
                    typeElement,
                    "The current version does not yet support the use of inheritance for types annotated with @Tx"
            );
        }
        return typeElement;
    }
}
