package org.babyfish.jimmer.apt.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ClassNames {

    private static final String[] EMPTY_STRING_ARR = new String[0];

    public static ClassName of(TypeElement typeElement, @Nullable Function<String, String> simpleNameConverter) {
        Collector collector = new Collector();
        collect(typeElement, collector);
        if (simpleNameConverter != null) {
            int index = collector.simpleNames.size() - 1;
            collector.simpleNames.set(index, simpleNameConverter.apply(collector.simpleNames.get(index)));
        }
        return ClassName.get(
                collector.packageName,
                collector.simpleNames.get(0),
                collector.simpleNames.subList(1, collector.simpleNames.size()).toArray(EMPTY_STRING_ARR)
        );
    }

    public static ClassName of(ClassName className, Function<String, String> simpleNameConverter) {
        List<String> simpleNames = new ArrayList<>(className.simpleNames());
        simpleNames.set(
                simpleNames.size() - 1,
                simpleNameConverter.apply(simpleNames.get(simpleNames.size() - 1))
        );
        return ClassName.get(
                className.packageName(),
                simpleNames.get(0),
                simpleNames.subList(1, simpleNames.size()).toArray(EMPTY_STRING_ARR)
        );
    }

    private static void collect(Element element, Collector collector) {
        if (element instanceof PackageElement) {
            collector.packageName = ((PackageElement) element).getQualifiedName().toString();
            return;
        }
        collector.simpleNames.add(0, element.getSimpleName().toString());
        collect(element.getEnclosingElement(), collector);
    }

    private static class Collector {
        String packageName;

        final List<String> simpleNames = new ArrayList<>();
    }
}
