package org.babyfish.jimmer.sql.kt.meta

import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.microservice.Order
import org.babyfish.jimmer.sql.kt.model.microservice.OrderItem
import org.babyfish.jimmer.sql.kt.model.microservice.Product
import kotlin.reflect.KClass

val BOOK_PROPS = props(Book::class)

val BOOK_STORE_PROPS = props(BookStore::class)

val MS_ORDER_PROPS = props(Order::class)

val MS_ORDER_ITEM_PROPS = props(OrderItem::class)

val MS_PRODUCT_PROPS = props(Product::class)

@Suppress("UNCHECKED_CAST")
private fun <T: Any> props(type: KClass<T>): Generated<T> =
    Generated(
        Class.forName(type.java.`package`.name + "." + type.simpleName + "PropsKt") as Class<T>
    )