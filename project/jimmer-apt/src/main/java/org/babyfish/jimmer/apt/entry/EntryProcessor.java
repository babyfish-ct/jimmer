package org.babyfish.jimmer.apt.entry;

import org.babyfish.jimmer.Immutable;
import org.babyfish.jimmer.apt.Context;
import org.babyfish.jimmer.sql.Embeddable;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.MappedSuperclass;

import javax.annotation.processing.Filer;
import javax.lang.model.element.TypeElement;
import java.util.Collection;
import java.util.Map;

public class EntryProcessor {

    private final Context context;

    private final Collection<TypeElement> typeElements;

    public EntryProcessor(Context context, Collection<TypeElement> typeElements) {
        this.context = context;
        this.typeElements = typeElements;
    }

    public void process() {

        PackageCollector packageCollector = new PackageCollector();

        IndexFileGenerator entityGenerator = new IndexFileGenerator(context, typeElements, packageCollector) {
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

        IndexFileGenerator immutableGenerator = new IndexFileGenerator(context, typeElements, packageCollector) {
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

        Filer filer = context.getFiler();
        if (!allElementMap.isEmpty()) {
            new ImmutablesGenerator(packageName, context.getImmutablesTypeName(), allElementMap.values(), filer).generate();
        }
        if (!entityElementMap.isEmpty()) {

            if (context.withTables()) {
                new TablesGenerator(packageName, context.getTablesTypeName(), entityElementMap.values(), filer, false).generate();
            }

            if (context.withTableExes()) {
                new TablesGenerator(packageName, context.getTableExesTypeName(), entityElementMap.values(), filer, true).generate();
            }


            if (context.withFetchers()) {
                new FetchersGenerator(packageName, context.getFetchersTypeName(), entityElementMap.values(), filer).generate();

            }
        }
    }
}
