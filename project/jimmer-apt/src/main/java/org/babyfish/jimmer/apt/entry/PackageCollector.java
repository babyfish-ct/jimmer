package org.babyfish.jimmer.apt.entry;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.*;
import java.util.regex.Pattern;

public class PackageCollector {

    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

    private List<String> pathParts;

    private String str;

    private final Map<String, TypeElement> elementMap = new TreeMap<>();

    public void accept(TypeElement typeElement) {
        elementMap.put(typeElement.getQualifiedName().toString(), typeElement);
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

    public Map<String, TypeElement> getElementMap() {
        return Collections.unmodifiableMap(elementMap);
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
