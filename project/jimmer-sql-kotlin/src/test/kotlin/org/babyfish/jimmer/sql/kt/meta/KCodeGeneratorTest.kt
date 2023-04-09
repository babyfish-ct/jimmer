package org.babyfish.jimmer.sql.kt.meta

import org.babyfish.jimmer.kt.toImmutableProp
import org.babyfish.jimmer.meta.TargetLevel
import org.babyfish.jimmer.sql.kt.ast.table.*
import org.babyfish.jimmer.sql.kt.model.classic.book.Book
import org.babyfish.jimmer.sql.kt.model.classic.store.BookStore
import org.babyfish.jimmer.sql.kt.model.microservice.Order
import org.babyfish.jimmer.sql.kt.model.microservice.OrderItem
import org.babyfish.jimmer.sql.kt.model.microservice.Product
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaMethod
import kotlin.test.Test
import kotlin.test.fail

class KCodeGeneratorTest {

    @Test
    fun testTable() {

        notGenerated(BOOK_PROPS, Book::name, KProps::class)
        generated(BOOK_PROPS, Book::name, KNonNullProps::class)
        generated(BOOK_PROPS, Book::name, KNullableProps::class)
        notGenerated(BOOK_PROPS, Book::name, KNonNullTable::class)
        notGenerated(BOOK_PROPS, Book::name, KNonNullTable::class)
        notGenerated(BOOK_PROPS, Book::name, KTableEx::class)
        notGenerated(BOOK_PROPS, Book::name, KNonNullTableEx::class)
        notGenerated(BOOK_PROPS, Book::name, KNonNullTableEx::class)

        generated(BOOK_STORE_PROPS, BookStore::website, KProps::class)
        notGenerated(BOOK_STORE_PROPS, BookStore::website, KNonNullProps::class)
        notGenerated(BOOK_STORE_PROPS, BookStore::website, KNullableProps::class)
        notGenerated(BOOK_STORE_PROPS, BookStore::website, KNonNullTable::class)
        notGenerated(BOOK_STORE_PROPS, BookStore::website, KNonNullTable::class)
        notGenerated(BOOK_STORE_PROPS, BookStore::website, KTableEx::class)
        notGenerated(BOOK_STORE_PROPS, BookStore::website, KNonNullTableEx::class)
        notGenerated(BOOK_STORE_PROPS, BookStore::website, KNonNullTableEx::class)

        generated(BOOK_PROPS, Book::store, KProps::class)
        notGenerated(BOOK_PROPS, Book::store, KNonNullProps::class)
        notGenerated(BOOK_PROPS, Book::store, KNullableProps::class)
        notGenerated(BOOK_PROPS, Book::store, KNonNullTable::class)
        notGenerated(BOOK_PROPS, Book::store, KNonNullTable::class)
        generated(BOOK_PROPS, Book::store, KTableEx::class)
        notGenerated(BOOK_PROPS, Book::store, KNonNullTableEx::class)
        notGenerated(BOOK_PROPS, Book::store, KNonNullTableEx::class)

        notGenerated(BOOK_PROPS, Book::authors, KProps::class)
        notGenerated(BOOK_PROPS, Book::authors, KNonNullProps::class)
        notGenerated(BOOK_PROPS, Book::authors, KNullableProps::class)
        notGenerated(BOOK_PROPS, Book::authors, KNonNullTable::class)
        notGenerated(BOOK_PROPS, Book::authors, KNonNullTable::class)
        generated(BOOK_PROPS, Book::authors, KTableEx::class)
        notGenerated(BOOK_PROPS, Book::authors, KNonNullTableEx::class)
        notGenerated(BOOK_PROPS, Book::authors, KNonNullTableEx::class)

        notGenerated(MS_ORDER_PROPS, Order::orderItems, KProps::class)
        notGenerated(MS_ORDER_PROPS, Order::orderItems, KNonNullProps::class)
        notGenerated(MS_ORDER_PROPS, Order::orderItems, KNullableProps::class)
        notGenerated(MS_ORDER_PROPS, Order::orderItems, KNonNullTable::class)
        notGenerated(MS_ORDER_PROPS, Order::orderItems, KNonNullTable::class)
        notGenerated(MS_ORDER_PROPS, Order::orderItems, KTableEx::class)
        notGenerated(MS_ORDER_PROPS, Order::orderItems, KNonNullTableEx::class)
        notGenerated(MS_ORDER_PROPS, Order::orderItems, KNonNullTableEx::class)

        generated(MS_ORDER_ITEM_PROPS, OrderItem::order, KProps::class)
        notGenerated(MS_ORDER_ITEM_PROPS, OrderItem::order, KNonNullProps::class)
        notGenerated(MS_ORDER_ITEM_PROPS, OrderItem::order, KNullableProps::class)
        notGenerated(MS_ORDER_ITEM_PROPS, OrderItem::order, KNonNullTable::class)
        notGenerated(MS_ORDER_ITEM_PROPS, OrderItem::order, KNonNullTable::class)
        notGenerated(MS_ORDER_ITEM_PROPS, OrderItem::order, KTableEx::class)
        notGenerated(MS_ORDER_ITEM_PROPS, OrderItem::order, KNonNullTableEx::class)
        notGenerated(MS_ORDER_ITEM_PROPS, OrderItem::order, KNonNullTableEx::class)

        notGenerated(MS_ORDER_ITEM_PROPS, OrderItem::products, KProps::class)
        notGenerated(MS_ORDER_ITEM_PROPS, OrderItem::products, KNonNullProps::class)
        notGenerated(MS_ORDER_ITEM_PROPS, OrderItem::products, KNullableProps::class)
        notGenerated(MS_ORDER_ITEM_PROPS, OrderItem::products, KNonNullTable::class)
        notGenerated(MS_ORDER_ITEM_PROPS, OrderItem::products, KNonNullTable::class)
        generated(MS_ORDER_ITEM_PROPS, OrderItem::products, KTableEx::class)
        notGenerated(MS_ORDER_ITEM_PROPS, OrderItem::products, KNonNullTableEx::class)
        notGenerated(MS_ORDER_ITEM_PROPS, OrderItem::products, KNonNullTableEx::class)

        notGenerated(MS_PRODUCT_PROPS, Product::orderItems, KProps::class)
        notGenerated(MS_PRODUCT_PROPS, Product::orderItems, KNonNullProps::class)
        notGenerated(MS_PRODUCT_PROPS, Product::orderItems, KNullableProps::class)
        notGenerated(MS_PRODUCT_PROPS, Product::orderItems, KNonNullTable::class)
        notGenerated(MS_PRODUCT_PROPS, Product::orderItems, KNonNullTable::class)
        notGenerated(MS_PRODUCT_PROPS, Product::orderItems, KTableEx::class)
        notGenerated(MS_PRODUCT_PROPS, Product::orderItems, KNonNullTableEx::class)
        notGenerated(MS_PRODUCT_PROPS, Product::orderItems, KNonNullTableEx::class)
    }

    companion object {

        @JvmStatic
        fun <E: Any> generated(generated: Generated<E>, prop: KProperty1<E, *>, vararg parameterTypes: KClass<*>) {
            for (name in names(prop)) {
                try {
                    generated.javaClass.getDeclaredMethod(
                        prop.getter.javaMethod!!.name,
                        *parameterTypes.map { it.java }.toTypedArray()
                    )
                } catch (ex: NoSuchMethodException) {
                    fail(
                        "There is no method \"" +
                            prop.getter.javaMethod!!.name +
                            "\" with parameters " +
                            parameterTypes.contentToString() +
                            " in type \"" +
                            generated.javaClass +
                            "\""
                    )
                }
            }
        }

        @JvmStatic
        fun <E: Any> notGenerated(generated: Generated<E>, prop: KProperty1<E, *>, vararg parameterTypes: KClass<*>) {
            for (name in names(prop)) {
                try {
                    val method = generated.javaClass.getDeclaredMethod(
                        prop.getter.javaMethod!!.name,
                        *parameterTypes.map { it.java }.toTypedArray()
                    )
                    fail(
                        "The method \"" +
                            method +
                            "\" should not be generated"
                    )
                } catch (ex: NoSuchMethodException) {
                }
            }
        }

        @JvmStatic
        fun names(prop: KProperty1<*, *>): Array<String> =
            prop.getter.javaMethod!!.name.let {
                if (prop.toImmutableProp().isAssociation(TargetLevel.ENTITY)) {
                    arrayOf(it, "$it?")
                } else {
                    arrayOf(it)
                }
            }
    }
}