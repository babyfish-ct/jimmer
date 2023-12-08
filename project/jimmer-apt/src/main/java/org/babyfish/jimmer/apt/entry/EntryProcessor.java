package org.babyfish.jimmer.apt.entry;

import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.sql.Embeddable;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.MappedSuperclass;

import javax.annotation.processing.Filer;
import javax.lang.model.element.*;
import java.util.*;

public class EntryProcessor {

    private final Context context;

    private final Collection<TypeElement> typeElements;

    private final Filer filer;

    public EntryProcessor(Context context, Collection<TypeElement> typeElements, Filer filer) {
        this.context = context;
        this.typeElements = typeElements;
        this.filer = filer;
    }

    public void process() {

        PackageCollector packageCollector = new PackageCollector();

        IndexFileGenerator entityGenerator = new IndexFileGenerator(context, typeElements, filer, packageCollector) {
            @Override
            protected String getListFilePath() {
                return "META-INF/jimmer/entities";
            }

            @Override
            protected boolean isManaged(TypeElement typeElement, boolean strict) {
                if (strict) {
                    return typeElement.getAnnotation(Entity.class) != null;
                }
                return typeElement.getAnnotation(MappedSuperclass.class) == null && context.isImmutable(typeElement);
            }
        };

        IndexFileGenerator immutableGenerator = new IndexFileGenerator(context, typeElements, filer, packageCollector) {
            @Override
            protected String getListFilePath() {
                return "META-INF/jimmer/immutables";
            }

            @Override
            protected boolean isManaged(TypeElement typeElement, boolean strict) {
                if (strict) {
                    return typeElement.getAnnotation(Immutable.class) != null ||
                            typeElement.getAnnotation(Embeddable.class) != null;
                }
                return typeElement.getAnnotation(MappedSuperclass.class) == null && context.isImmutable(typeElement);
            }
        };

        String packageName = packageCollector.toString();
        Map<String, TypeElement> allElementMap = packageCollector.getElementMap();
        Map<String, TypeElement> entityElementMap = entityGenerator.getElementMap();

        entityGenerator.generate();
        immutableGenerator.generate();

        new ObjectsGenerator(packageName, allElementMap.values(), filer).generate();
        new TablesGenerator(packageName, entityElementMap.values(), filer, false).generate();
        new TablesGenerator(packageName, entityElementMap.values(), filer, true).generate();
        new FetchersGenerator(packageName, entityElementMap.values(), filer).generate();
    }
}
