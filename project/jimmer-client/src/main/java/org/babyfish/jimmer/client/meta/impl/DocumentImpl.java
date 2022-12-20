package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.Doc;
import org.babyfish.jimmer.client.Docs;
import org.babyfish.jimmer.client.meta.Document;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.spi.EntityPropImplementor;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;

class DocumentImpl implements Document {

    private final List<Item> items;

    private DocumentImpl(List<Item> items) {
        this.items = items;
    }

    @Nullable
    static Document of(ImmutableProp prop) {
        return of(((EntityPropImplementor)prop).getJavaGetter());
    }

    @Nullable
    static Document of(AnnotatedElement annotatedElement) {
        if (annotatedElement == null) {
            return null;
        }
        List<Item> items = new ArrayList<>();
        Docs docs = annotatedElement.getAnnotation(Docs.class);
        if (docs != null) {
            for (Doc doc : docs.value()) {
                items.add(itemOf(doc));
            }
        } else {
            Doc doc = annotatedElement.getAnnotation(Doc.class);
            if (doc != null) {
                items.add(itemOf(doc));
            }
        }
        if (items.isEmpty()) {
            return null;
        }
        return new DocumentImpl(items);
    }

    static Item itemOf(Doc doc) {
        String text = doc.value();
        int size = text.length();
        int depth = 0;
        int index = 0;
        while (index < size) {
            char c = text.charAt(index);
            if (!Character.isWhitespace(c)) {
                if (c == '-') {
                    depth++;
                } else {
                    break;
                }
            }
            index++;
        }
        if (index != 0) {
            text = text.substring(index);
        }
        return new ItemImpl(text, depth);
    }

    @Override
    public List<Item> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return "Document{" +
                "items=" + items +
                '}';
    }

    private static class ItemImpl implements Item {

        private final String text;

        private final int depth;

        private ItemImpl(String text, int depth) {
            this.text = text;
            this.depth = depth;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public int getDepth() {
            return depth;
        }

        @Override
        public String toString() {
            return "DocumentItem{" +
                    "text='" + text + '\'' +
                    ", depth='" + depth + '\'' +
                    '}';
        }
    }
}
