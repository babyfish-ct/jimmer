package org.babyfish.jimmer.example.core;

import org.babyfish.jimmer.example.core.model.AuthorDraft;
import org.babyfish.jimmer.example.core.model.Book;
import org.babyfish.jimmer.example.core.model.BookDraft;

public class App {

    public static void main(String[] args) {

        Book book = BookDraft.$.produce(b -> {
            b.setName("book");
            b.setStore(s -> {
                s.setName("parent");
            });
            b.addIntoAuthors(a -> {
                a.setName("child-1");
            });
            b.addIntoAuthors(a -> {
                a.setName("child-2");
            });
        });

        Book book2 = BookDraft.$.produce(book, b -> {
            b.setName(b.name() + "!");
            b.store().setName(b.store().name() + "!");
            for (AuthorDraft author : b.authors(true)) {
                author.setName(author.name() + "!");
            }
        });

        System.out.println(book);
        System.out.println(book2);
    }
}
