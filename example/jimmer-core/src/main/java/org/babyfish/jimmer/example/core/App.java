package org.babyfish.jimmer.example.core;

import org.babyfish.jimmer.example.core.model.*;

public class App {

    public static void main(String[] args) {

        System.out.println("Book demo");
        System.out.println("-------------------");
        bookDemo();
        System.out.println();

        System.out.println("TreeNode demo");
        System.out.println("-------------------");
        treeNodeDemo();
        System.out.println();
    }

    private static void bookDemo() {

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
            for (AuthorDraft author : b.authors(false)) {
                author.setName(author.name() + "!");
            }
        });

        System.out.println("book: " + book);
        System.out.println("newBook: " + book2);
    }

    private static void treeNodeDemo() {

        // First step, create new object from scratch
        TreeNode treeNode = TreeNodeDraft.$.produce(root -> {
            root.setName("Root").addIntoChildNodes(food -> {
                food
                        .setName("Food")
                        .addIntoChildNodes(drink -> {
                            drink
                                    .setName("Drink")
                                    .addIntoChildNodes(cococola -> {
                                        cococola.setName("Coco Cola");
                                    })
                                    .addIntoChildNodes(fanta -> {
                                        fanta.setName("Fanta");
                                    });
                        });
                ;
            });
        });

        // Second step, make some "changes" based on the existing object to get a new object.
        TreeNode newTreeNode = TreeNodeDraft.$.produce(
                treeNode, // existing object
                root -> {
                    root
                            .childNodes(false).get(0) // Food
                            .childNodes(false).get(0) // Drink
                            .childNodes(false).get(0) // Coco Cola
                            .setName("Coco Cola plus");
                }
        );

        System.out.println("treeNode:" + treeNode);
        System.out.println("newTreeNode:" + newTreeNode);
    }
}
