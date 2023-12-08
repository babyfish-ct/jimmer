package org.babyfish.jimmer.apt.immutable;

import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.apt.entry.EntryProcessor;
import org.babyfish.jimmer.apt.immutable.generator.*;
import org.babyfish.jimmer.apt.immutable.meta.ImmutableType;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.HashMap;
import java.util.Map;

public class ImmutableProcessor {

    private final Context context;

    private final Filer filer;

    private final Messager messager;

    public ImmutableProcessor(Context context, Filer filer, Messager messager) {
        this.context = context;
        this.filer = filer;
        this.messager = messager;
    }

    public void process(RoundEnvironment roundEnv) {
        Map<TypeElement, ImmutableType> immutableTypeMap = parseImmutableTypes(roundEnv);
        generateJimmerTypes(immutableTypeMap, roundEnv);
    }

    private Map<TypeElement, ImmutableType> parseImmutableTypes(RoundEnvironment roundEnv) {
        Map<TypeElement, ImmutableType> map = new HashMap<>();
        for (Element element : roundEnv.getRootElements()) {
            if (element instanceof TypeElement) {
                TypeElement typeElement = (TypeElement) element;
                if (context.isImmutable(typeElement) && context.include(typeElement)) {
                    ImmutableType immutableType = context.getImmutableType(typeElement);
                    map.put(typeElement, immutableType);
                }
            }
        }
        return map;
    }

    private void generateJimmerTypes(Map<TypeElement, ImmutableType> immutableTypeMap, RoundEnvironment roundEnv) {
        for (ImmutableType immutableType : immutableTypeMap.values()) {
            new DraftGenerator(
                    immutableType,
                    filer
            ).generate();
            new PropsGenerator(
                    context,
                    immutableType,
                    filer
            ).generate();
            messager.printMessage(Diagnostic.Kind.NOTE, "Immutable: " + immutableType.getQualifiedName());
            if (immutableType.isEntity()) {
                messager.printMessage(Diagnostic.Kind.NOTE, "Entity: " + immutableType.getQualifiedName());
                new TableGenerator(
                        context,
                        immutableType,
                        false,
                        filer
                ).generate();
                new TableGenerator(
                        context,
                        immutableType,
                        true,
                        filer
                ).generate();
                new FetcherGenerator(
                        context,
                        immutableType,
                        filer
                ).generate();
            } else if (immutableType.isEmbeddable()) {
                new PropExpressionGenerator(
                        context,
                        immutableType,
                        filer
                ).generate();
            }
        }
        new EntryProcessor(
                context,
                immutableTypeMap.keySet(),
                filer
        ).process();
    }
}
