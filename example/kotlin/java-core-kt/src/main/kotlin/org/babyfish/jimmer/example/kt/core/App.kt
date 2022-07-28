package org.babyfish.jimmer.example.kt.core

import org.babyfish.jimmer.example.kt.core.model.Book
import org.babyfish.jimmer.example.kt.core.model.TreeNode
import org.babyfish.jimmer.example.kt.core.model.addBy
import org.babyfish.jimmer.example.kt.core.model.by
import org.babyfish.jimmer.kt.new

fun main(args: Array<String>) {
    println("Book demo")
    println("-------------------")
    bookDemo()
    println()

    println("TreeNode demo")
    println("-------------------")
    treeNodeDemo()
    println()
}

private fun bookDemo() {

    /*
     * First step, create new object from scratch
     */
    val book = new(Book::class).by {
        name = "book"
        store().name = "store"
        authors().addBy {
            name = "author-1"
        }
        authors().addBy {
            name = "author-2"
        }
    }

    /*
     * Second step, make some "changes" based on the existing object to get a new object.
     */
    val newBook = new(Book::class).by(book) {
        name += "!"
        store().name += "!"
        for (author in authors()) {
            author.name += "!"
        }
    }

    println("book: $book")
    println("newBook: $newBook")
}

private fun treeNodeDemo() {

    /*
     * First step, create new object from scratch
     */
    val treeNode = new(TreeNode::class).by {
        name = "Root"
        childNodes().addBy {
            name = "Food"
            childNodes().addBy {
                name = "Drinks"
                childNodes().addBy {
                    name = "Cococola"
                }
                childNodes().addBy {
                    name = "Fanta"
                }
            }
        }
    }

    /*
     * Second step, make some "changes" based on the existing object to get a new object.
     */
    val newTreeNode = new(TreeNode::class).by(treeNode) {
        childNodes()[0] // Food
            .childNodes()[0] // Drinks
            .childNodes()[0] // Cococola
            .name += " plus"
    }

    println("treeNode: $treeNode")
    println("newTreeNode: $newTreeNode")
}